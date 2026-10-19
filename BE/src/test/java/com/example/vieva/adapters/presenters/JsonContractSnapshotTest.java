package com.example.vieva.adapters.presenters;

import com.example.vieva.adapters.presenters.QuestionBankDtos.*;
import com.example.vieva.application.ports.output.ImportReport;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@JsonTest
class JsonContractSnapshotTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final Instant T1 = Instant.parse("2026-10-20T10:15:30Z");
    private static final Instant T2 = Instant.parse("2026-10-20T11:20:00Z");

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID U4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID U5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID U6 = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID U7 = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private UserDto sampleUserDto() {
        return UserDto.builder()
                .userId(U1)
                .email("lecturer@vieva.edu.vn")
                .userCode("LEC001")
                .fullName("Nguyen Van A")
                .phoneNumber("0987654321")
                .avatarUrl("https://res.cloudinary.com/avatar.png")
                .status("ACTIVE")
                .role("ROLE_LECTURER")
                .roles(Set.of("ROLE_LECTURER"))
                .mustChangePassword(false)
                .createdAt(T1)
                .updatedAt(T2)
                .build();
    }

    private CourseDocumentDto sampleCourseDocumentDto() {
        return CourseDocumentDto.builder()
                .documentId(U1)
                .subjectId(U2)
                .uploadedBy(U3)
                .fileName("lecture1.pdf")
                .fileUrl("https://storage.example.com/lecture1.pdf")
                .fileSizeBytes(102400L)
                .mimeType("application/pdf")
                .indexingStatus(DocumentStatus.READY)
                .errorMessage("None")
                .totalChunks(10)
                .indexAttempts(1)
                .createdAt(T1)
                .updatedAt(T2)
                .build();
    }

    private QuestionVersionDto sampleQuestionVersionDto() {
        RubricCriterionDto criterion = RubricCriterionDto.builder()
                .criterionId(U5)
                .name("Criterion 1")
                .description("Criterion description")
                .maxScore(new BigDecimal("5.0"))
                .levels(List.of(new PerformanceLevel("Good", "Good quality", new BigDecimal("5.0"))))
                .orderIndex(1)
                .build();

        RubricDto rubric = RubricDto.builder()
                .rubricId(U4)
                .name("Standard Rubric")
                .description("Rubric description")
                .totalScore(new BigDecimal("10.0"))
                .criteria(List.of(criterion))
                .build();

        QuestionSourceDto source = QuestionSourceDto.builder()
                .sourceId(U6)
                .chunkId(U7)
                .documentId(U1)
                .documentName("chapter1.pdf")
                .citationQuote("Quote text")
                .similarityScore(new BigDecimal("0.85"))
                .order(1)
                .build();

        return QuestionVersionDto.builder()
                .versionId(U1)
                .questionId(U2)
                .questionCode("CS101-Q01")
                .subjectId(U3)
                .topicId(U4)
                .topicName("Introduction")
                .versionNumber(1)
                .content("What is OOP?")
                .expectedAnswer("Object-Oriented Programming")
                .bloomLevel(BloomLevel.UNDERSTAND)
                .bloomLevelLabel("Thông hiểu")
                .bloomConfirmed(true)
                .origin(QuestionGenerationMode.AI_RAG)
                .status(QuestionStatus.APPROVED)
                .parentVersionId(U5)
                .generationRequestId(U6)
                .regenerationCount(2)
                .rejectReason("Needs more clarity")
                .createdBy(U3)
                .reviewedBy(U7)
                .reviewedAt(T1)
                .createdAt(T1)
                .updatedAt(T2)
                .lockVersion(1L)
                .rubric(rubric)
                .sources(List.of(source))
                .build();
    }

    @Test
    @DisplayName("Snapshot: AuthResponse JSON contract")
    void snapshotAuthResponse() throws Exception {
        AuthResponse dto = AuthResponse.builder()
                .accessToken("token-xyz")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(sampleUserDto())
                .build();

        String expectedJson = """
                {
                  "accessToken": "token-xyz",
                  "tokenType": "Bearer",
                  "expiresIn": 3600,
                  "user": {
                    "userId": "00000000-0000-0000-0000-000000000001",
                    "email": "lecturer@vieva.edu.vn",
                    "userCode": "LEC001",
                    "fullName": "Nguyen Van A",
                    "phoneNumber": "0987654321",
                    "avatarUrl": "https://res.cloudinary.com/avatar.png",
                    "status": "ACTIVE",
                    "role": "ROLE_LECTURER",
                    "roles": ["ROLE_LECTURER"],
                    "mustChangePassword": false,
                    "createdAt": "2026-10-20T10:15:30Z",
                    "updatedAt": "2026-10-20T11:20:00Z"
                  }
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: CourseDocumentDto JSON contract")
    void snapshotCourseDocumentDto() throws Exception {
        CourseDocumentDto dto = sampleCourseDocumentDto();
        String expectedJson = """
                {
                  "documentId": "00000000-0000-0000-0000-000000000001",
                  "subjectId": "00000000-0000-0000-0000-000000000002",
                  "uploadedBy": "00000000-0000-0000-0000-000000000003",
                  "fileName": "lecture1.pdf",
                  "fileUrl": "https://storage.example.com/lecture1.pdf",
                  "fileSizeBytes": 102400,
                  "mimeType": "application/pdf",
                  "indexingStatus": "READY",
                  "errorMessage": "None",
                  "totalChunks": 10,
                  "indexAttempts": 1,
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z"
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: DocumentPreviewDto JSON contract")
    void snapshotDocumentPreviewDto() throws Exception {
        DocumentPreviewDto dto = DocumentPreviewDto.builder()
                .document(sampleCourseDocumentDto())
                .chunks(List.of(
                        new DocumentPreviewDto.PreviewChunkDto("Chunk 1", "Sample text", false)
                ))
                .build();

        String expectedJson = """
                {
                  "document": {
                    "documentId": "00000000-0000-0000-0000-000000000001",
                    "subjectId": "00000000-0000-0000-0000-000000000002",
                    "uploadedBy": "00000000-0000-0000-0000-000000000003",
                    "fileName": "lecture1.pdf",
                    "fileUrl": "https://storage.example.com/lecture1.pdf",
                    "fileSizeBytes": 102400,
                    "mimeType": "application/pdf",
                    "indexingStatus": "READY",
                    "errorMessage": "None",
                    "totalChunks": 10,
                    "indexAttempts": 1,
                    "createdAt": "2026-10-20T10:15:30Z",
                    "updatedAt": "2026-10-20T11:20:00Z"
                  },
                  "chunks": [
                    {
                      "label": "Chunk 1",
                      "text": "Sample text",
                      "truncated": false
                    }
                  ]
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: LecturerSubjectDto JSON contract")
    void snapshotLecturerSubjectDto() throws Exception {
        LecturerSubjectDto dto = LecturerSubjectDto.builder()
                .lecturerSubjectId(U1)
                .lecturerId(U2)
                .subjectId(U3)
                .isActive(true)
                .assignedAt(T1)
                .assignedBy(U4)
                .revokedAt(T2)
                .build();

        String expectedJson = """
                {
                  "lecturerSubjectId": "00000000-0000-0000-0000-000000000001",
                  "lecturerId": "00000000-0000-0000-0000-000000000002",
                  "subjectId": "00000000-0000-0000-0000-000000000003",
                  "isActive": true,
                  "assignedAt": "2026-10-20T10:15:30Z",
                  "assignedBy": "00000000-0000-0000-0000-000000000004",
                  "revokedAt": "2026-10-20T11:20:00Z"
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: MessageResponse JSON contract")
    void snapshotMessageResponse() throws Exception {
        MessageResponse dto = new MessageResponse("Operation successful");
        String expectedJson = """
                {
                  "message": "Operation successful"
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: PageResponse JSON contract")
    void snapshotPageResponse() throws Exception {
        PageResponse<String> dto = PageResponse.<String>builder()
                .content(List.of("item-1", "item-2"))
                .page(0)
                .size(10)
                .totalElements(2L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        String expectedJson = """
                {
                  "content": ["item-1", "item-2"],
                  "page": 0,
                  "size": 10,
                  "totalElements": 2,
                  "totalPages": 1,
                  "first": true,
                  "last": true
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: QuestionVersionDto JSON contract")
    void snapshotQuestionVersionDto() throws Exception {
        QuestionVersionDto dto = sampleQuestionVersionDto();
        String expectedJson = """
                {
                  "versionId": "00000000-0000-0000-0000-000000000001",
                  "questionId": "00000000-0000-0000-0000-000000000002",
                  "questionCode": "CS101-Q01",
                  "subjectId": "00000000-0000-0000-0000-000000000003",
                  "topicId": "00000000-0000-0000-0000-000000000004",
                  "topicName": "Introduction",
                  "versionNumber": 1,
                  "content": "What is OOP?",
                  "expectedAnswer": "Object-Oriented Programming",
                  "bloomLevel": "UNDERSTAND",
                  "bloomLevelLabel": "Thông hiểu",
                  "bloomConfirmed": true,
                  "origin": "AI_RAG",
                  "status": "APPROVED",
                  "parentVersionId": "00000000-0000-0000-0000-000000000005",
                  "generationRequestId": "00000000-0000-0000-0000-000000000006",
                  "regenerationCount": 2,
                  "rejectReason": "Needs more clarity",
                  "createdBy": "00000000-0000-0000-0000-000000000003",
                  "reviewedBy": "00000000-0000-0000-0000-000000000007",
                  "reviewedAt": "2026-10-20T10:15:30Z",
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z",
                  "lockVersion": 1,
                  "rubric": {
                    "rubricId": "00000000-0000-0000-0000-000000000004",
                    "name": "Standard Rubric",
                    "description": "Rubric description",
                    "totalScore": 10.0,
                    "criteria": [
                      {
                        "criterionId": "00000000-0000-0000-0000-000000000005",
                        "name": "Criterion 1",
                        "description": "Criterion description",
                        "maxScore": 5.0,
                        "levels": [
                          {
                            "label": "Good",
                            "description": "Good quality",
                            "score": 5.0
                          }
                        ],
                        "orderIndex": 1
                      }
                    ]
                  },
                  "sources": [
                    {
                      "sourceId": "00000000-0000-0000-0000-000000000006",
                      "chunkId": "00000000-0000-0000-0000-000000000007",
                      "documentId": "00000000-0000-0000-0000-000000000001",
                      "documentName": "chapter1.pdf",
                      "citationQuote": "Quote text",
                      "similarityScore": 0.85,
                      "order": 1
                    }
                  ]
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    private String sampleQuestionVersionJson() {
        return """
                {
                  "versionId": "00000000-0000-0000-0000-000000000001",
                  "questionId": "00000000-0000-0000-0000-000000000002",
                  "questionCode": "CS101-Q01",
                  "subjectId": "00000000-0000-0000-0000-000000000003",
                  "topicId": "00000000-0000-0000-0000-000000000004",
                  "topicName": "Introduction",
                  "versionNumber": 1,
                  "content": "What is OOP?",
                  "expectedAnswer": "Object-Oriented Programming",
                  "bloomLevel": "UNDERSTAND",
                  "bloomLevelLabel": "Thông hiểu",
                  "bloomConfirmed": true,
                  "origin": "AI_RAG",
                  "status": "APPROVED",
                  "parentVersionId": "00000000-0000-0000-0000-000000000005",
                  "generationRequestId": "00000000-0000-0000-0000-000000000006",
                  "regenerationCount": 2,
                  "rejectReason": "Needs more clarity",
                  "createdBy": "00000000-0000-0000-0000-000000000003",
                  "reviewedBy": "00000000-0000-0000-0000-000000000007",
                  "reviewedAt": "2026-10-20T10:15:30Z",
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z",
                  "lockVersion": 1,
                  "rubric": {
                    "rubricId": "00000000-0000-0000-0000-000000000004",
                    "name": "Standard Rubric",
                    "description": "Rubric description",
                    "totalScore": 10.0,
                    "criteria": [
                      {
                        "criterionId": "00000000-0000-0000-0000-000000000005",
                        "name": "Criterion 1",
                        "description": "Criterion description",
                        "maxScore": 5.0,
                        "levels": [
                          {
                            "label": "Good",
                            "description": "Good quality",
                            "score": 5.0
                          }
                        ],
                        "orderIndex": 1
                      }
                    ]
                  },
                  "sources": [
                    {
                      "sourceId": "00000000-0000-0000-0000-000000000006",
                      "chunkId": "00000000-0000-0000-0000-000000000007",
                      "documentId": "00000000-0000-0000-0000-000000000001",
                      "documentName": "chapter1.pdf",
                      "citationQuote": "Quote text",
                      "similarityScore": 0.85,
                      "order": 1
                    }
                  ]
                }
                """;
    }

    @Test
    @DisplayName("Snapshot: QuestionBankItemDto JSON contract")
    void snapshotQuestionBankItemDto() throws Exception {
        QuestionBankItemDto dto = QuestionBankItemDto.builder()
                .questionId(U1)
                .questionCode("CS101-Q01")
                .subjectId(U2)
                .topicId(U3)
                .topicName("Topic 1")
                .status(QuestionLifecycleStatus.ACTIVE)
                .hasPendingDraft(false)
                .currentVersion(sampleQuestionVersionDto())
                .createdAt(T1)
                .updatedAt(T2)
                .build();

        String expectedJson = """
                {
                  "questionId": "00000000-0000-0000-0000-000000000001",
                  "questionCode": "CS101-Q01",
                  "subjectId": "00000000-0000-0000-0000-000000000002",
                  "topicId": "00000000-0000-0000-0000-000000000003",
                  "topicName": "Topic 1",
                  "status": "ACTIVE",
                  "hasPendingDraft": false,
                  "currentVersion": %s,
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z"
                }
                """.formatted(sampleQuestionVersionJson());

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: QuestionDetailDto JSON contract")
    void snapshotQuestionDetailDto() throws Exception {
        QuestionVersionDto version = sampleQuestionVersionDto();
        QuestionDetailDto dto = QuestionDetailDto.builder()
                .questionId(U1)
                .questionCode("CS101-Q01")
                .subjectId(U2)
                .topicId(U3)
                .status(QuestionLifecycleStatus.ACTIVE)
                .currentApprovedVersionId(U4)
                .currentVersion(version)
                .pendingDraft(version)
                .history(List.of(version))
                .createdAt(T1)
                .updatedAt(T2)
                .build();

        String versionJson = sampleQuestionVersionJson();
        String expectedJson = """
                {
                  "questionId": "00000000-0000-0000-0000-000000000001",
                  "questionCode": "CS101-Q01",
                  "subjectId": "00000000-0000-0000-0000-000000000002",
                  "topicId": "00000000-0000-0000-0000-000000000003",
                  "status": "ACTIVE",
                  "currentApprovedVersionId": "00000000-0000-0000-0000-000000000004",
                  "currentVersion": %s,
                  "pendingDraft": %s,
                  "history": [%s],
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z"
                }
                """.formatted(versionJson, versionJson, versionJson);

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: GenerationRequestDto JSON contract")
    void snapshotGenerationRequestDto() throws Exception {
        Map<BloomLevel, Integer> dist = new LinkedHashMap<>();
        dist.put(BloomLevel.REMEMBER, 2);
        dist.put(BloomLevel.UNDERSTAND, 3);

        GenerationRequestDto dto = GenerationRequestDto.builder()
                .generationRequestId(U1)
                .subjectId(U2)
                .topicId(U3)
                .documentIds(List.of(U4))
                .bloomDistribution(dist)
                .totalQuestions(5)
                .lecturerNote("Note")
                .status(QuestionGenerationStatus.COMPLETED)
                .generatedCount(5)
                .rejectedCount(0)
                .attemptCount(1)
                .issues(List.of("Minor issue"))
                .errorMessage("No error")
                .createdAt(T1)
                .updatedAt(T2)
                .drafts(List.of(sampleQuestionVersionDto()))
                .build();

        String expectedJson = """
                {
                  "generationRequestId": "00000000-0000-0000-0000-000000000001",
                  "subjectId": "00000000-0000-0000-0000-000000000002",
                  "topicId": "00000000-0000-0000-0000-000000000003",
                  "documentIds": ["00000000-0000-0000-0000-000000000004"],
                  "bloomDistribution": {
                    "REMEMBER": 2,
                    "UNDERSTAND": 3
                  },
                  "totalQuestions": 5,
                  "lecturerNote": "Note",
                  "status": "COMPLETED",
                  "generatedCount": 5,
                  "rejectedCount": 0,
                  "attemptCount": 1,
                  "issues": ["Minor issue"],
                  "errorMessage": "No error",
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z",
                  "drafts": [%s]
                }
                """.formatted(sampleQuestionVersionJson());

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: TopicDto JSON contract")
    void snapshotTopicDto() throws Exception {
        TopicDto dto = TopicDto.builder()
                .topicId(U1)
                .subjectId(U2)
                .name("Topic 1")
                .description("Description 1")
                .orderIndex(1)
                .build();

        String expectedJson = """
                {
                  "topicId": "00000000-0000-0000-0000-000000000001",
                  "subjectId": "00000000-0000-0000-0000-000000000002",
                  "name": "Topic 1",
                  "description": "Description 1",
                  "orderIndex": 1
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: SubjectDto JSON contract")
    void snapshotSubjectDto() throws Exception {
        SubjectDto dto = SubjectDto.builder()
                .subjectId(U1)
                .headLecturerId(U2)
                .subjectCode("PRJ301")
                .subjectName("Java Web")
                .description("Java Web Course")
                .credits(3)
                .status("ACTIVE")
                .createdAt(T1)
                .updatedAt(T2)
                .build();

        String expectedJson = """
                {
                  "subjectId": "00000000-0000-0000-0000-000000000001",
                  "headLecturerId": "00000000-0000-0000-0000-000000000002",
                  "subjectCode": "PRJ301",
                  "subjectName": "Java Web",
                  "description": "Java Web Course",
                  "credits": 3,
                  "status": "ACTIVE",
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z"
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: UserDto JSON contract")
    void snapshotUserDto() throws Exception {
        UserDto dto = sampleUserDto();
        String expectedJson = """
                {
                  "userId": "00000000-0000-0000-0000-000000000001",
                  "email": "lecturer@vieva.edu.vn",
                  "userCode": "LEC001",
                  "fullName": "Nguyen Van A",
                  "phoneNumber": "0987654321",
                  "avatarUrl": "https://res.cloudinary.com/avatar.png",
                  "status": "ACTIVE",
                  "role": "ROLE_LECTURER",
                  "roles": ["ROLE_LECTURER"],
                  "mustChangePassword": false,
                  "createdAt": "2026-10-20T10:15:30Z",
                  "updatedAt": "2026-10-20T11:20:00Z"
                }
                """;

        String actualJson = objectMapper.writeValueAsString(dto);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: PagedResult<Notification> direct response JSON contract")
    void snapshotPagedResultNotification() throws Exception {
        Notification notification = new Notification(
                U1,
                U2,
                U3,
                U4,
                "QUESTION_APPROVED",
                "Your question was approved",
                T1,
                T2
        );

        PagedResult<Notification> paged = PagedResult.<Notification>builder()
                .content(List.of(notification))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        String expectedJson = """
                {
                  "content": [
                    {
                      "notificationId": "00000000-0000-0000-0000-000000000001",
                      "recipientId": "00000000-0000-0000-0000-000000000002",
                      "questionId": "00000000-0000-0000-0000-000000000003",
                      "versionId": "00000000-0000-0000-0000-000000000004",
                      "type": "QUESTION_APPROVED",
                      "message": "Your question was approved",
                      "createdAt": "2026-10-20T10:15:30Z",
                      "readAt": "2026-10-20T11:20:00Z"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "totalElements": 1,
                  "totalPages": 1,
                  "first": true,
                  "last": true
                }
                """;

        String actualJson = objectMapper.writeValueAsString(paged);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Snapshot: ImportReport direct response JSON contract")
    void snapshotImportReport() throws Exception {
        ImportReport report = ImportReport.builder()
                .dryRun(true)
                .totalRows(10)
                .totalQuestions(5)
                .validQuestions(4)
                .invalidQuestions(1)
                .createdQuestions(0)
                .errors(List.of(
                        new ImportReport.RowError(2, "Content", "Q2", "Content too short")
                ))
                .createdQuestionIds(List.of(U1))
                .build();

        String expectedJson = """
                {
                  "dryRun": true,
                  "totalRows": 10,
                  "totalQuestions": 5,
                  "validQuestions": 4,
                  "invalidQuestions": 1,
                  "createdQuestions": 0,
                  "errors": [
                    {
                      "row": 2,
                      "column": "Content",
                      "questionRef": "Q2",
                      "message": "Content too short"
                    }
                  ],
                  "createdQuestionIds": [
                    "00000000-0000-0000-0000-000000000001"
                  ]
                }
                """;

        String actualJson = objectMapper.writeValueAsString(report);
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }
}
