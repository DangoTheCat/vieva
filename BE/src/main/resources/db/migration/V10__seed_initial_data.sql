-- ============================================================================
-- V10: Baseline seed data for development and evaluation testing
-- Creates default roles, administrator, lecturers, student, sample subjects,
-- lecturer assignments, topics, course document with chunks, questions,
-- rubrics, and criteria.
-- ============================================================================

-- crypt() / gen_salt() below need pgcrypto
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Roles
INSERT INTO roles (role_id, role_code, role_name, description) VALUES
    (1, 'ROLE_ADMIN', 'Quản trị viên hệ thống', 'Toàn quyền cấu hình hệ thống, người dùng và môn học'),
    (2, 'ROLE_LECTURER', 'Giảng viên', 'Biên soạn ngân hàng câu hỏi, upload tài liệu RAG, duyệt rubric và chấm thi'),
    (3, 'ROLE_STUDENT', 'Sinh viên / Thí sinh', 'Tham gia phòng thi vấn đáp, nộp bài và nhận phản hồi đánh giá'),
    (4, 'ROLE_USER', 'Người dùng hệ thống', 'Tài khoản xác thực cơ bản'),
    (5, 'ADMIN', 'Quản trị viên (Alias)', 'Alias tương thích cho ROLE_ADMIN'),
    (6, 'LECTURER', 'Giảng viên (Alias)', 'Alias tương thích cho ROLE_LECTURER'),
    (7, 'STUDENT', 'Sinh viên (Alias)', 'Alias tương thích cho ROLE_STUDENT')
ON CONFLICT (role_id) DO NOTHING;

-- 2. Users (Password: password123 for all users)
INSERT INTO users (user_id, email, user_code, full_name, password_hash, phone_number, status, version) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'admin@aives.edu.vn', 'ADMIN001', 'Hệ thống Quản trị viên', crypt('password123', gen_salt('bf', 10)), '0901234567', 'ACTIVE', 0),
    ('b0000000-0000-0000-0000-000000000001', 'vancee@fpt.edu.vn', 'GV001', 'TS. Edward Vance', crypt('password123', gen_salt('bf', 10)), '0912345678', 'ACTIVE', 0),
    ('b0000000-0000-0000-0000-000000000002', 'vancem@fpt.edu.vn', 'GV002', 'ThS. Maria Vance', crypt('password123', gen_salt('bf', 10)), '0923456789', 'ACTIVE', 0),
    ('c0000000-0000-0000-0000-000000000001', 'student@fpt.edu.vn', 'SE170001', 'Nguyễn Văn An', crypt('password123', gen_salt('bf', 10)), '0934567890', 'ACTIVE', 0)
ON CONFLICT (user_id) DO NOTHING;

-- 3. User Roles
INSERT INTO user_roles (user_id, role_id, assigned_by) VALUES
    ('a0000000-0000-0000-0000-000000000001', 1, 'a0000000-0000-0000-0000-000000000001'),
    ('a0000000-0000-0000-0000-000000000001', 5, 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000001', 2, 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000001', 6, 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000002', 2, 'a0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000002', 6, 'a0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', 3, 'a0000000-0000-0000-0000-000000000001'),
    ('c0000000-0000-0000-0000-000000000001', 7, 'a0000000-0000-0000-0000-000000000001')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 4. Subjects
INSERT INTO subjects (subject_id, subject_code, subject_name, description, credits, status) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'SWD392', 'Kiến trúc và Thiết kế Phần mềm (Software Architecture and Design)', 'Môn học chuyên sâu về kiến trúc phần mềm, Clean Architecture, Onion Architecture, Microservices và thiết kế hệ thống phân tán chịu tải cao.', 3, 'ACTIVE'),
    ('d0000000-0000-0000-0000-000000000002', 'PRN211', 'Lập trình C# cơ bản với .NET (Basic Cross-Platform .NET Application)', 'Nhập môn lập trình hướng đối tượng với C#, cấu trúc dữ liệu cơ bản, LINQ và làm việc với Entity Framework Core.', 3, 'ACTIVE'),
    ('d0000000-0000-0000-0000-000000000003', 'PRN231', 'Xây dựng Web API với ASP.NET Core (Building RESTful APIs with ASP.NET Core)', 'Thiết kế và phát triển RESTful Web APIs chuẩn công nghiệp với ASP.NET Core 8, JWT Authentication và Swagger OpenAPI.', 3, 'ACTIVE')
ON CONFLICT (subject_id) DO NOTHING;

-- 5. Lecturer Assignments (Course-scoped RBAC)
INSERT INTO lecturer_subjects (lecturer_subject_id, lecturer_id, subject_id, is_active, assigned_by) VALUES
    ('e1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', TRUE, 'a0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', TRUE, 'a0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', TRUE, 'a0000000-0000-0000-0000-000000000001'),
    ('e1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', TRUE, 'a0000000-0000-0000-0000-000000000001')
ON CONFLICT (lecturer_subject_id) DO NOTHING;

-- 6. Topics
INSERT INTO topics (topic_id, subject_id, topic_name, order_index, description) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Onion Architecture & Domain-Driven Design', 1, 'Cấu trúc phân tầng đồng tâm, phân lập Domain Core, Application Services và Adapter Infrastructure.'),
    ('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'Microservices & Distributed Systems Pattern', 2, 'Giao dịch phân tán, Saga Pattern, Transactional Outbox Pattern và CQRS.'),
    ('e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'Real-time Protocols & WebSockets STOMP', 3, 'Giao thức truyền phát âm thanh hai chiều, STOMP frames, cơ chế Reconnect 60s và heartbeat.'),
    ('e0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000002', 'C# Language Fundamentals & OOP', 1, 'Kế thừa, đa hình, interface, abstract class, record types và pattern matching trong C#.'),
    ('e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000002', 'LINQ & Entity Framework Core', 2, 'Kỹ thuật truy vấn dữ liệu với LINQ to Entities, tối ưu hóa IQueryable, AsNoTracking và Migration.'),
    ('e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000003', 'RESTful API Architecture & HTTP Standards', 1, 'Chuẩn thiết kế HTTP Methods, Status Codes, Idempotency và Content Negotiation.')
ON CONFLICT (topic_id) DO NOTHING;

-- 7. Course Documents
INSERT INTO course_documents (document_id, subject_id, uploaded_by, file_name, file_url, file_size_bytes, mime_type, indexing_status, total_chunks, extracted_text_summary) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'SWD392_Architecture_Guide.pdf', 'https://storage.aives.edu.vn/courses/swd392/docs/architecture_guide.pdf', 1450280, 'application/pdf', 'READY', 3, 'Tài liệu hướng dẫn thực hành kiến trúc phần mềm nâng cao: Onion Architecture, Transactional Outbox Pattern và WebSocket STOMP.')
ON CONFLICT (document_id) DO NOTHING;

-- 8. Document Chunks
INSERT INTO document_chunks (chunk_id, document_id, chunk_index, content, token_count, metadata_json) VALUES
    ('f1000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 0, 'Onion Architecture đặt Domain Entity và Enterprise Business Rules ở trung tâm lõi. Mọi luồng phụ thuộc mã nguồn bắt buộc phải hướng tâm (Inward Dependency). Các tầng ngoài như Web API Controller và PostgreSQL Database chỉ đóng vai trò Adapter và phụ thuộc vào Interface Ports định nghĩa tại Application Core theo Dependency Inversion Principle (DIP).', 180, '{"chapter": "1", "page": 12, "title": "Onion Architecture Inward Dependencies"}'::jsonb),
    ('f1000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 1, 'Transactional Outbox Pattern giải quyết bài toán Dual-Write và đảm bảo At-Least-Once Delivery trong hệ thống phân tán. Khi có sự kiện kinh doanh phát sinh, bản ghi sự kiện được ghi vào bảng outbox_events trong cùng một cơ sở dữ liệu transaction với nghiệp vụ chính. Một tiến trình nền (Relay Worker) quét bảng này và xuất sang Message Broker, loại bỏ hoàn toàn nguy cơ mất dữ liệu khi broker gặp sự cố tạm thời.', 195, '{"chapter": "3", "page": 45, "title": "Transactional Outbox Pattern"}'::jsonb),
    ('f1000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', 2, 'Giao thức STOMP trên WebSocket hỗ trợ tương tác thoại hai chiều thời gian thực. Trong trường hợp thí sinh bị rớt mạng do sự cố kết nối, hệ thống kích hoạt cửa sổ cứu vớt (Reconnect Recovery Window) 60 giây. Session thi được giữ ở trạng thái chờ; nếu thí sinh tái kết nối thành công trước 60 giây, phiên thi tiếp tục bình thường; nếu quá 60 giây, phiên thi tự động kết thúc với trạng thái INTERRUPTED.', 175, '{"chapter": "4", "page": 78, "title": "Real-time Viva STOMP Protocols"}'::jsonb)
ON CONFLICT (chunk_id) DO NOTHING;

-- 9. Questions
INSERT INTO questions (question_id, subject_id, topic_id, question_code, status, created_by) VALUES
    ('10000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'Q-SWD-001', 'ACTIVE', 'b0000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 'Q-SWD-002', 'ACTIVE', 'b0000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'Q-SWD-003', 'ACTIVE', 'b0000000-0000-0000-0000-000000000001')
ON CONFLICT (question_id) DO NOTHING;

-- 10. Question Versions
INSERT INTO question_versions (question_version_id, question_id, version_number, question_content, reference_answer, bloom_level, bloom_confirmed, generation_mode, approval_status, reviewed_by, reviewed_at, created_by) VALUES
    (
        '11000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        1,
        'Hãy phân tích nguyên lý Dependency Inversion trong kiến trúc Củ hành (Onion Architecture) và chỉ rõ tầng nào nắm giữ vai trò trung tâm?',
        'Nguyên lý Dependency Inversion yêu cầu tầng trung tâm Domain (chứa Entities và Enterprise Business Rules) hoàn toàn độc lập, không phụ thuộc vào bất kỳ thư viện hay cơ sở hạ tầng nào bên ngoài. Các tầng bên ngoài như Infrastructure và UI đều phụ thuộc hướng tâm vào Domain thông qua abstraction interfaces (Ports).',
        'UNDERSTAND',
        TRUE,
        'MANUAL',
        'APPROVED',
        'b0000000-0000-0000-0000-000000000001',
        NOW(),
        'b0000000-0000-0000-0000-000000000001'
    ),
    (
        '11000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        1,
        'So sánh cơ chế đảm bảo tính nhất quán dữ liệu giữa Transactional Outbox Pattern và Two-Phase Commit (2PC) trong kiến trúc phân tán?',
        '2PC là giao thức đồng bộ (blocking), yêu cầu coordinator và các participants phải duy trì lock cho đến khi hoàn thành commit, rất dễ gây nghẽn và chịu rủi ro điểm lỗi đơn (Single Point of Failure). Transactional Outbox Pattern là giải pháp bất đồng bộ (eventual consistency), ghi bản tin vào bảng outbox cục bộ trong cùng một transaction với nghiệp vụ chính, sau đó background poller xuất message sang message broker, đảm bảo At-Least-Once Delivery mà không block tài nguyên hệ thống.',
        'ANALYZE',
        TRUE,
        'MANUAL',
        'APPROVED',
        'b0000000-0000-0000-0000-000000000001',
        NOW(),
        'b0000000-0000-0000-0000-000000000001'
    ),
    (
        '11000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000003',
        1,
        'Trình bày cách xử lý sự cố rớt mạng trong phòng thi vấn đáp trực tuyến bằng cơ chế Reconnect Recovery và cửa sổ cứu vớt 60 giây?',
        'Khi kết nối WebSocket bị đứt, client kích hoạt bộ đếm ngược 60 giây Reconnect Window. Session state trên server được giữ nguyên ở trạng thái INTERRUPTED tạm thời. Nếu thí sinh tái kết nối thành công trong 60 giây, server cấp lại snapshot câu hỏi hiện tại và tiếp tục phiên thi; nếu quá 60 giây, lượt thi được chốt tự động với trạng thái INTERRUPTED.',
        'APPLY',
        FALSE,
        'AI_RAG',
        'DRAFT',
        NULL,
        NULL,
        'b0000000-0000-0000-0000-000000000001'
    )
ON CONFLICT (question_version_id) DO NOTHING;

-- Cập nhật current_approved_version_id
UPDATE questions SET current_approved_version_id = '11000000-0000-0000-0000-000000000001' WHERE question_id = '10000000-0000-0000-0000-000000000001';
UPDATE questions SET current_approved_version_id = '11000000-0000-0000-0000-000000000002' WHERE question_id = '10000000-0000-0000-0000-000000000002';

-- 11. Question Sources
INSERT INTO question_sources (question_source_id, question_version_id, chunk_id, citation_quote, similarity_score) VALUES
    ('13000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'Onion Architecture đặt Domain Entity và Enterprise Business Rules ở trung tâm lõi... phụ thuộc vào Interface Ports định nghĩa tại Application Core theo Dependency Inversion Principle.', 0.945),
    ('13000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000002', 'Transactional Outbox Pattern giải quyết bài toán Dual-Write và đảm bảo At-Least-Once Delivery trong hệ thống phân tán...', 0.920),
    ('13000000-0000-0000-0000-000000000003', '11000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000003', 'Trong trường hợp thí sinh bị rớt mạng do sự cố kết nối, hệ thống kích hoạt cửa sổ cứu vớt (Reconnect Recovery Window) 60 giây...', 0.912)
ON CONFLICT (question_source_id) DO NOTHING;

-- 12. Rubrics
INSERT INTO rubrics (rubric_id, question_version_id, rubric_name, total_points, description) VALUES
    ('12000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', 'Rubric Đánh giá Hiểu biết Kiến trúc Củ hành (Onion Architecture)', 10.00, 'Đánh giá mức độ nhận thức về phân tầng hướng tâm và nguyên lý Dependency Inversion.'),
    ('12000000-0000-0000-0000-000000000002', '11000000-0000-0000-0000-000000000002', 'Rubric Đánh giá Phân tích Mô hình Phân tán (Distributed Patterns)', 10.00, 'Đánh giá khả năng phân tích trade-off giữa tính sẵn sàng và tính nhất quán dữ liệu.'),
    ('12000000-0000-0000-0000-000000000003', '11000000-0000-0000-0000-000000000003', 'Rubric Đánh giá Xử lý Sự cố Reconnect 60s', 10.00, 'Đánh giá khả năng vận dụng kiến thức giao thức mạng và quy trình bảo toàn phiên thi.')
ON CONFLICT (rubric_id) DO NOTHING;

-- 13. Rubric Criteria
INSERT INTO rubric_criteria (criterion_id, rubric_id, criterion_name, max_points, achievement_descriptors, order_index) VALUES
    ('14000000-0000-0000-0000-000000000001', '12000000-0000-0000-0000-000000000001', 'Nhận diện đúng vai trò độc lập của Domain Layer', 4.00, 'Chỉ rõ Domain Core ở vị trí trung tâm, không phụ thuộc framework hay DB bên ngoài.', 1),
    ('14000000-0000-0000-0000-000000000002', '12000000-0000-0000-0000-000000000001', 'Giải thích chính xác chiều phụ thuộc Dependency Inversion', 4.00, 'Trình bày cách tầng ngoài đảo ngược phụ thuộc vào Interfaces của Domain/Application.', 2),
    ('14000000-0000-0000-0000-000000000003', '12000000-0000-0000-0000-000000000001', 'Kỹ năng trình bày và diễn đạt mạch lạc', 2.00, 'Thuật ngữ chính xác, phong thái tự tin, phản xạ vấn đáp tốt.', 3),
    ('14000000-0000-0000-0000-000000000004', '12000000-0000-0000-0000-000000000002', 'Phân tích bản chất Blocking của Two-Phase Commit', 4.00, 'Nêu rõ nguy cơ treo tài nguyên và điểm lỗi đơn coordinator.', 1),
    ('14000000-0000-0000-0000-000000000005', '12000000-0000-0000-0000-000000000002', 'Lập luận giải pháp At-Least-Once Delivery của Outbox Pattern', 4.00, 'Mô tả rõ việc ghi bảng outbox trong cùng 1 local transaction và background relay.', 2),
    ('14000000-0000-0000-0000-000000000006', '12000000-0000-0000-0000-000000000002', 'Đánh giá trade-off tính nhất quán Eventual Consistency', 2.00, 'So sánh sự đánh đổi giữa tính khả dụng cao và độ trễ đồng bộ.', 3),
    ('14000000-0000-0000-0000-000000000007', '12000000-0000-0000-0000-000000000003', 'Quy trình đếm ngược 60s và giữ session state', 5.00, 'Trình bày trạng thái INTERRUPTED tạm thời và khôi phục khi reconnect.', 1),
    ('14000000-0000-0000-0000-000000000008', '12000000-0000-0000-0000-000000000003', 'Phân định kịch bản thành công và quá hạn timeout', 5.00, 'Chỉ rõ mốc 60s kết thúc phiên nếu thí sinh không quay lại kịp thời.', 2)
ON CONFLICT (criterion_id) DO NOTHING;
