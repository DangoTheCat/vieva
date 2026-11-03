package com.example.vieva.integration;

import com.example.vieva.application.ports.output.AttemptGradingGateway;
import com.example.vieva.application.ports.output.AttemptGradingResult;
import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.RuleBasedGraderPort;
import com.example.vieva.application.ports.output.TransactionRunnerPort;
import com.example.vieva.application.usecases.exam.AttemptGradingServiceImpl;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.TurnStatus;
import com.example.vieva.domain.entities.TurnType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.autoconfigure.exclude=",
        "vieva.ai.provider=mock",
        "vieva.storage.provider=local"
})
@Testcontainers
class AttemptGradingConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/migration"), "/opt/vieva-schema")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/init.sh"), "/docker-entrypoint-initdb.d/01-vieva.sh");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ExamAttemptRepository examAttemptRepository;
    @Autowired private ExamScoreRepository examScoreRepository;
    @Autowired private ExamAttemptQuestionSnapshotRepository snapshotRepository;
    @Autowired private RubricRepository rubricRepository;
    @Autowired private RuleBasedGraderPort ruleBasedGraderPort;
    @Autowired private TransactionRunnerPort transactionRunner;

    @Test
    @DisplayName("P2 Real PostgreSQL Concurrency: Two threads finalizing same attempt via barriers assert exactly one ExamScore and no active DB transaction during gateway wait")
    void concurrentGradingUnderPostgreSQL_assertSingleScoreAndNoLockDuringGatewayWait() throws Exception {
        UUID student = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        String suffix = student.toString().substring(0, 8);

        jdbc.update("INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?,?,?,?,?, 'ACTIVE')",
                student, "st" + suffix + "@fpt.edu.vn", "ST" + suffix, "Student " + suffix, "{noop}x");
        jdbc.update("INSERT INTO subjects (subject_id, subject_code, subject_name, status) VALUES (?,?,?, 'ACTIVE')",
                subject, "SUB" + suffix, "Subject " + suffix);
        jdbc.update("INSERT INTO exam_attempts (id, student_id, subject_id, status, current_question_index, total_questions) VALUES (?,?,?, 'FINALIZING', 0, 1)",
                attemptId, student, subject);

        UUID turnId = UUID.randomUUID();
        jdbc.update("INSERT INTO exam_turns (id, attempt_id, turn_order, question_text, transcript_text, status, score, feedback) " +
                        "VALUES (?, ?, 1, 'Sample Question', 'Student Answer', 'EVALUATED', 8.50, 'Good response')",
                turnId, attemptId);

        ExamAttempt attempt = examAttemptRepository.findById(attemptId).orElseThrow();
        ExamTurn turn = ExamTurn.builder()
                .id(turnId)
                .attemptId(attemptId)
                .turnOrder(1)
                .status(TurnStatus.EVALUATED)
                .turnType(TurnType.MAIN_QUESTION)
                .score(new BigDecimal("8.50"))
                .transcriptText("Student Answer")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CyclicBarrier gatewayEntryBarrier = new CyclicBarrier(2);
        CountDownLatch testThreadProceedLatch = new CountDownLatch(1);
        CountDownLatch gatewayReleaseLatch = new CountDownLatch(1);
        AtomicInteger gatewayCalls = new AtomicInteger(0);

        AttemptGradingGateway delayedGateway = (turnsList, rubricSummary) -> {
            int callIndex = gatewayCalls.incrementAndGet();
            try {
                if (callIndex == 2) {
                    testThreadProceedLatch.countDown();
                }
                gatewayEntryBarrier.await(10, TimeUnit.SECONDS);
                gatewayReleaseLatch.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new AttemptGradingResult(
                    new BigDecimal("8.50"),
                    "EXCELLENT",
                    "Concurrent grading verified",
                    "GEMINI_FLASH_LITE"
            );
        };

        AttemptGradingServiceImpl concurrentService = new AttemptGradingServiceImpl(
                delayedGateway,
                ruleBasedGraderPort,
                rubricRepository,
                examAttemptRepository,
                examScoreRepository,
                snapshotRepository,
                transactionRunner
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ExamScore> future1 = executor.submit(() -> concurrentService.gradeAndCompleteAttempt(attempt, List.of(turn)));
            Future<ExamScore> future2 = executor.submit(() -> concurrentService.gradeAndCompleteAttempt(attempt, List.of(turn)));

            boolean enteredGateway = testThreadProceedLatch.await(10, TimeUnit.SECONDS);
            assertThat(enteredGateway).isTrue();

            // PROOF 1: Zero active transaction or idle-in-transaction connection in PostgreSQL during external gateway wait
            Integer idleInTxCount = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND pid != pg_backend_pid() AND state = 'idle in transaction'",
                    Integer.class
            );
            assertThat(idleInTxCount).isZero();

            // PROOF 2: Database row can be read and inspected without being locked
            assertThatCode(() -> {
                String currentStatus = jdbc.queryForObject(
                        "SELECT status FROM exam_attempts WHERE id = ?",
                        String.class,
                        attemptId
                );
                assertThat(currentStatus).isEqualTo("FINALIZING");
            }).doesNotThrowAnyException();

            // Release both threads to proceed to Step 3
            gatewayReleaseLatch.countDown();

            ExamScore score1 = future1.get(15, TimeUnit.SECONDS);
            ExamScore score2 = future2.get(15, TimeUnit.SECONDS);

            // Assertions on outcomes
            assertThat(score1).isNotNull();
            assertThat(score2).isNotNull();
            assertThat(score1.getId()).isEqualTo(score2.getId());
            assertThat(score1.getTotalScore()).isEqualByComparingTo(new BigDecimal("8.50"));
            assertThat(score2.getTotalScore()).isEqualByComparingTo(new BigDecimal("8.50"));

            // PROOF 3: Exactly ONE ExamScore persisted in PostgreSQL database
            Integer scoreRowCount = jdbc.queryForObject(
                    "SELECT count(*) FROM exam_scores WHERE attempt_id = ?",
                    Integer.class,
                    attemptId
            );
            assertThat(scoreRowCount).isEqualTo(1);

            // PROOF 4: ExamAttempt in PostgreSQL has consistent final state COMPLETED with recorded score
            String finalDbStatus = jdbc.queryForObject(
                    "SELECT status FROM exam_attempts WHERE id = ?",
                    String.class,
                    attemptId
            );
            assertThat(finalDbStatus).isEqualTo("COMPLETED");

            BigDecimal finalTotalScore = jdbc.queryForObject(
                    "SELECT total_score FROM exam_attempts WHERE id = ?",
                    BigDecimal.class,
                    attemptId
            );
            assertThat(finalTotalScore).isEqualByComparingTo(new BigDecimal("8.50"));

        } finally {
            executor.shutdownNow();
        }
    }
}
