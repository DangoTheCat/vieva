-- ============================================================================
-- V14: Only three roles remain — ROLE_ADMIN, ROLE_LECTURER, ROLE_STUDENT.
-- ROLE_USER is merged into ROLE_STUDENT; the legacy alias rows (ADMIN, LECTURER,
-- STUDENT, USER) are folded into their canonical ROLE_* role and dropped.
-- ============================================================================

-- 1. Make sure the three canonical roles exist (fresh DBs get them from V10).
--    V10 inserts explicit role_ids, so move the SERIAL sequence past them first.
SELECT setval(pg_get_serial_sequence('roles', 'role_id'), GREATEST((SELECT MAX(role_id) FROM roles), 1));

INSERT INTO roles (role_code, role_name, description)
SELECT v.role_code, v.role_name, v.description
FROM (VALUES
    ('ROLE_ADMIN', 'Quản trị viên hệ thống', 'Toàn quyền cấu hình hệ thống, người dùng và môn học'),
    ('ROLE_LECTURER', 'Giảng viên', 'Biên soạn ngân hàng câu hỏi, upload tài liệu RAG, duyệt rubric và chấm thi'),
    ('ROLE_STUDENT', 'Sinh viên / Thí sinh', 'Tham gia phòng thi vấn đáp, nộp bài và nhận phản hồi đánh giá')
) AS v(role_code, role_name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_code = v.role_code);

-- 2. Move every account off a role that is going away (one row per user since V13)
UPDATE user_roles ur
SET role_id = target.role_id
FROM roles old_role, roles target
WHERE ur.role_id = old_role.role_id
  AND old_role.role_code NOT IN ('ROLE_ADMIN', 'ROLE_LECTURER', 'ROLE_STUDENT')
  AND target.role_code = CASE REPLACE(old_role.role_code, 'ROLE_', '')
                             WHEN 'ADMIN' THEN 'ROLE_ADMIN'
                             WHEN 'LECTURER' THEN 'ROLE_LECTURER'
                             ELSE 'ROLE_STUDENT'
                         END;

-- 3. Drop the merged roles and keep the set closed
DELETE FROM roles WHERE role_code NOT IN ('ROLE_ADMIN', 'ROLE_LECTURER', 'ROLE_STUDENT');

ALTER TABLE roles
    ADD CONSTRAINT ck_roles_known_codes CHECK (role_code IN ('ROLE_ADMIN', 'ROLE_LECTURER', 'ROLE_STUDENT'));
