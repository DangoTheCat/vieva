package com.example.vieva.integration;

import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.StudentExamService;
import com.example.vieva.application.ports.input.exam.SubmitTurnAnswerUseCase;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.output.ExamAttemptQuestionSnapshotRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.infrastructure.service.ExamFinalizationWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-at-least-32-characters-long-key",
        "spring.autoconfigure.exclude=",
        "vieva.ai.provider=mock",
        "vieva.storage.provider=local"
})
@Testcontainers
class ExamAttemptSnapshotIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/migration"), "/opt/vieva-schema")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/init.sh"), "/docker-entrypoint-initdb.d/01-vieva.sh");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ExamAttemptQuestionSnapshotRepository snapshotRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private StudentExamService studentExamService;
    @Autowired private ExamFinalizationWorker examFinalizationWorker;
    @Autowired private SubmitTurnAnswerUseCase submitTurnAnswerUseCase;
    @Autowired private ExamScoreRepository examScoreRepository;

    private record AttemptFixture(UUID studentId, UUID subjectId, UUID attemptId) {}

    private record ExamSetupFixture(UUID studentId, UUID lecturerId, UUID subjectId, UUID examId, UUID questionId, UUID versionId) {}

    private AttemptFixture createAttemptFixture() {
        UUID student = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        String suffix = student.toString().substring(0, 8);

        jdbc.update("INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?,?,?,?,?, 'ACTIVE')",
                student, "st" + suffix + "@fpt.edu.vn", "ST" + suffix, "Sinh vien " + suffix, "{noop}x");
        jdbc.update("INSERT INTO subjects (subject_id, subject_code, subject_name, status) VALUES (?,?,?, 'ACTIVE')",
                subject, "SUB" + suffix, "Mon hoc " + suffix);
        jdbc.update("INSERT INTO exam_attempts (id, student_id, subject_id, status, current_question_index, total_questions) VALUES (?,?,?, 'IN_PROGRESS', 0, 2)",
                attempt, student, subject);

        return new AttemptFixture(student, subject, attempt);
    }

    private ExamSetupFixture createActiveExamFixture() {
        UUID student = UUID.randomUUID();
        UUID lecturer = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID exam = UUID.randomUUID();
        UUID question = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        String suffix = student.toString().substring(0, 8);

        jdbc.update("INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?,?,?,?,?, 'ACTIVE')",
                student, "st" + suffix + "@fpt.edu.vn", "ST" + suffix, "Sinh vien " + suffix, "{noop}x");
        jdbc.update("INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?,?,?,?,?, 'ACTIVE')",
                lecturer, "lec" + suffix + "@fpt.edu.vn", "LEC" + suffix, "Giang vien " + suffix, "{noop}x");
        jdbc.update("INSERT INTO subjects (subject_id, subject_code, subject_name, status) VALUES (?,?,?, 'ACTIVE')",
                subject, "SUB" + suffix, "Mon hoc " + suffix);
        jdbc.update("INSERT INTO topics (topic_id, subject_id, topic_name, order_index) VALUES (?,?,?, 1)",
                topic, subject, "Chu de " + suffix);
        jdbc.update("INSERT INTO questions (question_id, topic_id, question_code, status, created_by, subject_id) VALUES (?,?,?, 'ACTIVE',?,?)",
                question, topic, "QC" + suffix, lecturer, subject);
        jdbc.update("INSERT INTO question_versions (question_version_id, question_id, version_number, question_content, reference_answer, bloom_level, generation_mode, approval_status, created_by) VALUES (?,?, 1, 'Question text', 'Expected answer', 'REMEMBER', 'MANUAL', 'APPROVED', ?)",
                version, question, lecturer);
        jdbc.update("UPDATE questions SET current_approved_version_id = ? WHERE question_id = ?", version, question);
        jdbc.update("INSERT INTO exams (id, subject_id, creator_id, title, duration_seconds, status) VALUES (?,?,?, 'Midterm Exam', 900, 'IN_PROGRESS')",
                exam, subject, lecturer);
        jdbc.update("INSERT INTO exam_questions (id, exam_id, question_id, question_version_id, order_index) VALUES (?,?,?,?, 0)",
                UUID.randomUUID(), exam, question, version);

        return new ExamSetupFixture(student, lecturer, subject, exam, question, version);
    }

    @Test
    @DisplayName("Persists and retrieves JSONB rubric_snapshot correctly with native PostgreSQL jsonb")
    void persistsAndLoadsJsonbRubricSnapshotCorrectly() {
        AttemptFixture fixture = createAttemptFixture();
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        String jsonRubric = "[{\"name\":\"Clarity\",\"weight\":0.4,\"description\":\"Clear explanation\"}," +
                "{\"name\":\"Accuracy\",\"weight\":0.6,\"description\":\"Factual correctness\"}]";

        ExamAttemptQuestionSnapshot snapshot = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(),
                questionId,
                versionId,
                0,
                "What is dependency injection?",
                "DI is a pattern where an object receives other objects that it depends on.",
                jsonRubric
        );

        snapshotRepository.save(snapshot);

        // Verify retrieval via repository
        Optional<ExamAttemptQuestionSnapshot> loadedOpt = snapshotRepository.findById(snapshot.getId());
        assertThat(loadedOpt).isPresent();
        ExamAttemptQuestionSnapshot loaded = loadedOpt.get();
        assertThat(loaded.getQuestionText()).isEqualTo("What is dependency injection?");
        assertThat(loaded.getExpectedAnswer()).isEqualTo("DI is a pattern where an object receives other objects that it depends on.");
        assertThat(loaded.getRubricSnapshot()).contains("Clarity").contains("Accuracy");

        // Verify native PostgreSQL JSONB type and queryability
        String pgJsonType = jdbc.queryForObject(
                "SELECT pg_typeof(rubric_snapshot)::text FROM exam_attempt_question_snapshots WHERE id = ?",
                String.class,
                snapshot.getId()
        );
        assertThat(pgJsonType).isEqualTo("jsonb");

        Integer elementCount = jdbc.queryForObject(
                "SELECT jsonb_array_length(rubric_snapshot) FROM exam_attempt_question_snapshots WHERE id = ?",
                Integer.class,
                snapshot.getId()
        );
        assertThat(elementCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Unique constraint on (attempt_id, order_index) prevents duplicate order indexes per attempt")
    void uniqueConstraintOrderIndexPerAttemptEnforced() {
        AttemptFixture fixture = createAttemptFixture();
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();

        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(), q1, UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        snapshotRepository.save(snap1);

        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(), q2, UUID.randomUUID(), 0, "Q2", "A2", "[]" // Same order index 0
        );

        assertThatThrownBy(() -> snapshotRepository.save(snap2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Transaction rollback completely removes saved snapshot records")
    void transactionRollbackRemovesPersistedSnapshots() {
        AttemptFixture fixture = createAttemptFixture();
        UUID questionId = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        ExamAttemptQuestionSnapshot snapshot = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(), questionId, UUID.randomUUID(), 0, "Rollback Q", "A", "[]"
        );

        assertThatThrownBy(() -> tx.execute(status -> {
            snapshotRepository.save(snapshot);
            throw new RuntimeException("Forced rollback");
        })).isInstanceOf(RuntimeException.class);

        assertThat(snapshotRepository.findById(snapshot.getId())).isEmpty();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exam_attempt_question_snapshots WHERE attempt_id = ?",
                Integer.class,
                fixture.attemptId()
        );
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Cascade delete on exam_attempts removes all related snapshots")
    void cascadeDeleteOnAttemptRemovesSnapshots() {
        AttemptFixture fixture = createAttemptFixture();
        ExamAttemptQuestionSnapshot snap1 = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(), UUID.randomUUID(), UUID.randomUUID(), 0, "Q1", "A1", "[]"
        );
        ExamAttemptQuestionSnapshot snap2 = ExamAttemptQuestionSnapshot.create(
                fixture.attemptId(), UUID.randomUUID(), UUID.randomUUID(), 1, "Q2", "A2", "[]"
        );
        snapshotRepository.save(snap1);
        snapshotRepository.save(snap2);

        List<ExamAttemptQuestionSnapshot> before = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(fixture.attemptId());
        assertThat(before).hasSize(2);

        // Delete parent attempt
        jdbc.update("DELETE FROM exam_attempts WHERE id = ?", fixture.attemptId());

        List<ExamAttemptQuestionSnapshot> after = snapshotRepository.findByAttemptIdOrderByOrderIndexAsc(fixture.attemptId());
        assertThat(after).isEmpty();
    }

    @Test
    @DisplayName("V23 schema: teacher grading columns on exam_turns and audit columns on exam_scores persist to PostgreSQL")
    void teacherGradingAndOverrideAuditPersistsToPostgreSQL() {
        AttemptFixture fixture = createAttemptFixture();
        UUID turnId = UUID.randomUUID();
        UUID lecturerId = fixture.studentId(); // reuse user

        // Insert turn with teacher grading
        jdbc.update(
                "INSERT INTO exam_turns (id, attempt_id, turn_order, question_text, status, teacher_score, teacher_feedback, teacher_id, teacher_graded_at) " +
                        "VALUES (?, ?, 1, 'Test Q', 'EVALUATED', ?, ?, ?, NOW())",
                turnId, fixture.attemptId(), new BigDecimal("8.50"), "Good explanation", lecturerId
        );

        BigDecimal teacherScore = jdbc.queryForObject(
                "SELECT teacher_score FROM exam_turns WHERE id = ?",
                BigDecimal.class,
                turnId
        );
        assertThat(teacherScore).isEqualByComparingTo(new BigDecimal("8.50"));

        String feedback = jdbc.queryForObject(
                "SELECT teacher_feedback FROM exam_turns WHERE id = ?",
                String.class,
                turnId
        );
        assertThat(feedback).isEqualTo("Good explanation");

        // Insert score with override reason
        UUID scoreId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO exam_scores (id, attempt_id, total_score, grade, scoring_mode, override_total_score, override_reason) " +
                        "VALUES (?, ?, ?, 'GOOD', 'GEMINI_FLASH_LITE', ?, ?)",
                scoreId, fixture.attemptId(), new BigDecimal("7.00"), new BigDecimal("9.00"), "Special commendation"
        );

        String reason = jdbc.queryForObject(
                "SELECT override_reason FROM exam_scores WHERE id = ?",
                String.class,
                scoreId
        );
        assertThat(reason).isEqualTo("Special commendation");
    }

    @Test
    @DisplayName("Concurrent enterExam creates exactly one attempt and snapshot set, returning identical attempt views")
    void concurrentEnterExamCreatesSingleAttemptAndSnapshots() throws Exception {
        ExamSetupFixture fixture = createActiveExamFixture();
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ExamAttemptView>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                return studentExamService.enterExam(fixture.examId(), fixture.studentId());
            }));
        }

        startGate.countDown();
        List<ExamAttemptView> results = new ArrayList<>();
        for (Future<ExamAttemptView> f : futures) {
            results.add(f.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(results).hasSize(2);
        UUID attemptId1 = results.get(0).attemptId();
        UUID attemptId2 = results.get(1).attemptId();
        assertThat(attemptId1).isEqualTo(attemptId2);

        Integer attemptCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exam_attempts WHERE exam_id = ? AND student_id = ?",
                Integer.class,
                fixture.examId(),
                fixture.studentId()
        );
        assertThat(attemptCount).isEqualTo(1);

        Integer snapshotCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM exam_attempt_question_snapshots WHERE attempt_id = ?",
                Integer.class,
                attemptId1
        );
        assertThat(snapshotCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Rollback suppresses finalization dispatch; afterCommit completes attempt and persists ExamScore idempotently")
    void finalizationPostCommitRollbackSuppressionAndSuccess() {
        AttemptFixture fixture = createAttemptFixture();
        UUID turnId = UUID.randomUUID();
        jdbc.update("INSERT INTO exam_turns (id, attempt_id, turn_order, question_text, status, score) VALUES (?, ?, 1, 'Q1', 'EVALUATED', 8.5)",
                turnId, fixture.attemptId());

        // 1. Rollback test: dispatchFinalization inside a rolling-back transaction
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.execute(status -> {
            examFinalizationWorker.dispatchFinalization(fixture.attemptId());
            throw new RuntimeException("Rollback simulation");
        })).isInstanceOf(RuntimeException.class);

        // Score was not created because transaction rolled back
        Optional<ExamScore> scoreAfterRollback = examScoreRepository.findByAttemptId(fixture.attemptId());
        assertThat(scoreAfterRollback).isEmpty();

        // 2. Direct executeFinalization moves attempt from FINALIZING to COMPLETED
        jdbc.update("UPDATE exam_attempts SET status = 'FINALIZING' WHERE id = ?", fixture.attemptId());
        examFinalizationWorker.executeFinalization(fixture.attemptId());

        Optional<ExamScore> scoreAfterExec = examScoreRepository.findByAttemptId(fixture.attemptId());
        assertThat(scoreAfterExec).isPresent();
        assertThat(scoreAfterExec.get().getTotalScore()).isNotNull();

        String status = jdbc.queryForObject("SELECT status FROM exam_attempts WHERE id = ?", String.class, fixture.attemptId());
        assertThat(status).isEqualTo("COMPLETED");

        // 3. Repeat executeFinalization is idempotent and does not fail or alter score
        examFinalizationWorker.executeFinalization(fixture.attemptId());
        Optional<ExamScore> scoreAfterRepeat = examScoreRepository.findByAttemptId(fixture.attemptId());
        assertThat(scoreAfterRepeat).isPresent();
        assertThat(scoreAfterRepeat.get().getId()).isEqualTo(scoreAfterExec.get().getId());
    }

    @Test
    @DisplayName("Submit answer replay: submitting already evaluated turn on COMPLETED attempt returns recorded view idempotently")
    void submitAnswerReplayReturnsRecordedResultWithoutReevaluating() {
        AttemptFixture fixture = createAttemptFixture();
        UUID turnId = UUID.randomUUID();
        jdbc.update("INSERT INTO exam_turns (id, attempt_id, turn_order, question_text, transcript_text, status, score, feedback) " +
                        "VALUES (?, ?, 1, 'Q1', 'My answer', 'EVALUATED', 9.0, 'Excellent')",
                turnId, fixture.attemptId());
        jdbc.update("UPDATE exam_attempts SET status = 'COMPLETED' WHERE id = ?", fixture.attemptId());

        TurnEvaluationView replayView = submitTurnAnswerUseCase.submitAnswer(
                fixture.attemptId(),
                turnId,
                fixture.studentId(),
                "New transcript that should be ignored",
                null,
                null
        );

        assertThat(replayView).isNotNull();
        assertThat(replayView.turnId()).isEqualTo(turnId);
        assertThat(replayView.score()).isEqualByComparingTo(new BigDecimal("9.00"));
        assertThat(replayView.feedback()).isEqualTo("Excellent");
        assertThat(replayView.transcriptText()).isEqualTo("My answer");
        assertThat(replayView.isAttemptFinished()).isTrue();
    }

    @Test
    @DisplayName("Genuine isolated V19 to V24 schema upgrade regression: preserves legacy records and validates new DDL/constraints")
    void v19ToV23SchemaUpgradeVerification() throws Exception {
        try (Connection con = java.util.Objects.requireNonNull(jdbc.getDataSource(), "DataSource must not be null").getConnection()) {
            try (Statement stmt = con.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS v19_upgrade_iso CASCADE;");
                stmt.execute("CREATE SCHEMA v19_upgrade_iso;");
                stmt.execute("SET search_path TO v19_upgrade_iso, public;");

                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath*:db/migration/V*.sql");
                if (resources.length == 0) {
                    resources = resolver.getResources("classpath*:db/migration/*.sql");
                }
                if (resources.length == 0) {
                    resources = resolver.getResources("classpath:db/migration/*.sql");
                }

                record MigrationScript(int version, String filename, Resource resource) {}

                Pattern vPattern = Pattern.compile("^[Vv](\\d+)");
                java.util.Map<String, MigrationScript> scriptMap = new java.util.LinkedHashMap<>();
                for (Resource res : resources) {
                    String name = res.getFilename();
                    if (name == null) continue;
                    Matcher m = vPattern.matcher(name);
                    if (m.find()) {
                        int ver = Integer.parseInt(m.group(1));
                        scriptMap.putIfAbsent(name, new MigrationScript(ver, name, res));
                    }
                }
                List<MigrationScript> allScripts = new ArrayList<>(scriptMap.values());
                allScripts.sort(Comparator.comparingInt(MigrationScript::version));
                assertThat(allScripts).as("Migration scripts must be found on classpath").isNotEmpty();

                List<MigrationScript> v0ToV19 = allScripts.stream().filter(s -> s.version() <= 19).toList();
                List<MigrationScript> v20ToV24 = allScripts.stream().filter(s -> s.version() >= 20).toList();

                // 1. Execute V0..V19 in order in isolated schema
                for (MigrationScript s : v0ToV19) {
                    stmt.execute("SET search_path TO v19_upgrade_iso, public;");
                    String rawSql = new String(s.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String sql = rawSql.replace("\uFEFF", "").trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }

                // 2. Insert representative records under V19 schema
                UUID studentId = UUID.randomUUID();
                UUID subjectId = UUID.randomUUID();
                UUID attemptId = UUID.randomUUID();
                UUID turnId = UUID.randomUUID();
                UUID scoreId = UUID.randomUUID();
                String suffix = studentId.toString().substring(0, 8);

                // User
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users (user_id, email, user_code, full_name, password_hash, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')")) {
                    ps.setObject(1, studentId);
                    ps.setString(2, "legacy_st_" + suffix + "@fpt.edu.vn");
                    ps.setString(3, "ST" + suffix);
                    ps.setString(4, "Sinh vien " + suffix);
                    ps.setString(5, "{noop}secret");
                    ps.executeUpdate();
                }

                // Subject
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO subjects (subject_id, subject_code, subject_name, status) VALUES (?, ?, ?, 'ACTIVE')")) {
                    ps.setObject(1, subjectId);
                    ps.setString(2, "SUB" + suffix);
                    ps.setString(3, "Mon hoc " + suffix);
                    ps.executeUpdate();
                }

                // ExamAttempt
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO exam_attempts (id, student_id, subject_id, status, current_question_index, total_questions) VALUES (?, ?, ?, 'IN_PROGRESS', 0, 2)")) {
                    ps.setObject(1, attemptId);
                    ps.setObject(2, studentId);
                    ps.setObject(3, subjectId);
                    ps.executeUpdate();
                }

                // ExamTurn
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO exam_turns (id, attempt_id, turn_order, question_text, transcript_text, status, score, feedback) VALUES (?, ?, 1, 'V19 question', 'V19 transcript', 'EVALUATED', 8.50, 'V19 feedback')")) {
                    ps.setObject(1, turnId);
                    ps.setObject(2, attemptId);
                    ps.executeUpdate();
                }

                // ExamScore
                boolean hasGradeCol;
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name FROM information_schema.columns WHERE table_schema = 'v19_upgrade_iso' AND table_name = 'exam_scores' AND column_name = 'grade'")) {
                    hasGradeCol = rs.next();
                }
                String gradeCol = hasGradeCol ? "grade" : "grade_level";
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO exam_scores (id, attempt_id, total_score, " + gradeCol + ", scoring_mode) VALUES (?, ?, 8.50, 'GOOD', 'RULE_BASED_FALLBACK')")) {
                    ps.setObject(1, scoreId);
                    ps.setObject(2, attemptId);
                    ps.executeUpdate();
                }

                // 3. Execute V20..V24 in version order into isolated schema
                for (MigrationScript s : v20ToV24) {
                    stmt.execute("SET search_path TO v19_upgrade_iso, public;");
                    String rawSql = new String(s.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String sql = rawSql.replace("\uFEFF", "").trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                    if (s.version() == 20) {
                        // Seed non-default legacy voice 'giahuy' into V20 speech_settings
                        stmt.execute("UPDATE speech_settings SET voice = 'giahuy' WHERE id = '00000000-0000-0000-0000-000000000001';");
                    }
                }

                stmt.execute("SET search_path TO v19_upgrade_iso, public;");

                // 4. Assert old records are preserved intact without data loss
                try (PreparedStatement ps = con.prepareStatement("SELECT email, full_name, user_code FROM users WHERE user_id = ?")) {
                    ps.setObject(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("email")).isEqualTo("legacy_st_" + suffix + "@fpt.edu.vn");
                        assertThat(rs.getString("full_name")).isEqualTo("Sinh vien " + suffix);
                        assertThat(rs.getString("user_code")).isEqualTo("ST" + suffix);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT subject_code, subject_name FROM subjects WHERE subject_id = ?")) {
                    ps.setObject(1, subjectId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("subject_code")).isEqualTo("SUB" + suffix);
                        assertThat(rs.getString("subject_name")).isEqualTo("Mon hoc " + suffix);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT status, total_questions, current_question_index FROM exam_attempts WHERE id = ?")) {
                    ps.setObject(1, attemptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("status")).isEqualTo("IN_PROGRESS");
                        assertThat(rs.getInt("total_questions")).isEqualTo(2);
                        assertThat(rs.getInt("current_question_index")).isEqualTo(0);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT question_text, transcript_text, score, feedback FROM exam_turns WHERE id = ?")) {
                    ps.setObject(1, turnId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("question_text")).isEqualTo("V19 question");
                        assertThat(rs.getString("transcript_text")).isEqualTo("V19 transcript");
                        assertThat(rs.getBigDecimal("score")).isEqualByComparingTo(new BigDecimal("8.50"));
                        assertThat(rs.getString("feedback")).isEqualTo("V19 feedback");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("SELECT total_score, scoring_mode FROM exam_scores WHERE id = ?")) {
                    ps.setObject(1, scoreId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getBigDecimal("total_score")).isEqualByComparingTo(new BigDecimal("8.50"));
                        assertThat(rs.getString("scoring_mode")).isEqualTo("RULE_BASED_FALLBACK");
                    }
                }

                // 5. Assert new columns and tables exist
                List<String> attemptCols = getColumnsForTable(stmt, "exam_attempts");
                assertThat(attemptCols).contains("paused_at", "remaining_seconds", "grace_deadline", "exam_id");

                List<String> docCols = getColumnsForTable(stmt, "course_documents");
                assertThat(docCols).contains("topic_id");

                List<String> tables = getTablesInSchema(stmt);
                assertThat(tables).contains("exams", "exam_questions", "exam_attempt_question_snapshots");

                List<String> turnCols = getColumnsForTable(stmt, "exam_turns");
                assertThat(turnCols).contains("teacher_score", "teacher_feedback", "teacher_id", "teacher_graded_at");

                List<String> scoreCols = getColumnsForTable(stmt, "exam_scores");
                assertThat(scoreCols).contains("ai_score", "ai_feedback", "teacher_score", "teacher_feedback", "teacher_id",
                        "override_total_score", "override_by", "override_at", "override_reason");

                List<String> speechCols = getColumnsForTable(stmt, "speech_settings");
                assertThat(speechCols).contains("stt_language", "tts_language", "vietnamese_voice", "english_voice");

                // Assert that non-default legacy voice 'giahuy' was preserved by V24 backfill instead of overwritten by default 'maichi'
                try (PreparedStatement ps = con.prepareStatement("SELECT voice, vietnamese_voice FROM speech_settings WHERE id = '00000000-0000-0000-0000-000000000001'")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("voice")).isEqualTo("giahuy");
                        assertThat(rs.getString("vietnamese_voice")).isEqualTo("giahuy");
                    }
                }

                // 6. Test JSONB and uniqueness constraints on upgraded schema
                UUID snapshotId = UUID.randomUUID();
                UUID testQuestionId = UUID.randomUUID();
                UUID testVersionId = UUID.randomUUID();
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO exam_attempt_question_snapshots (id, attempt_id, question_id, question_version_id, order_index, question_text, expected_answer, rubric_snapshot) " +
                                "VALUES (?, ?, ?, ?, 0, 'Test Q', 'Expected', '{\"criteria\": [{\"name\": \"accuracy\", \"weight\": 1.0}]}'::jsonb)")) {
                    ps.setObject(1, snapshotId);
                    ps.setObject(2, attemptId);
                    ps.setObject(3, testQuestionId);
                    ps.setObject(4, testVersionId);
                    ps.executeUpdate();
                }

                // JSONB extraction query
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT rubric_snapshot->'criteria'->0->>'name' AS crit_name FROM exam_attempt_question_snapshots WHERE id = ?")) {
                    ps.setObject(1, snapshotId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("crit_name")).isEqualTo("accuracy");
                    }
                }

                // Uniqueness constraint enforcement (same attempt_id and question_id)
                UUID dupSnapshotId = UUID.randomUUID();
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO exam_attempt_question_snapshots (id, attempt_id, question_id, question_version_id, order_index, question_text, expected_answer, rubric_snapshot) " +
                                "VALUES (?, ?, ?, ?, 1, 'Dup Q', 'Expected', '{}'::jsonb)")) {
                    ps.setObject(1, dupSnapshotId);
                    ps.setObject(2, attemptId);
                    ps.setObject(3, testQuestionId); // Duplicate question for attempt
                    ps.setObject(4, testVersionId);
                    assertThatThrownBy(ps::executeUpdate)
                            .isInstanceOf(Exception.class);
                }

            } finally {
                try (Statement cleanup = con.createStatement()) {
                    cleanup.execute("SET search_path TO public;");
                    cleanup.execute("DROP SCHEMA IF EXISTS v19_upgrade_iso CASCADE;");
                } catch (Exception ignored) {
                }
            }
        }
    }

    private List<String> getColumnsForTable(Statement stmt, String tableName) throws Exception {
        List<String> cols = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'v19_upgrade_iso' AND table_name = '" + tableName + "'")) {
            while (rs.next()) {
                cols.add(rs.getString(1));
            }
        }
        return cols;
    }

    private List<String> getTablesInSchema(Statement stmt) throws Exception {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'v19_upgrade_iso'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }
}
