-- ============================================================================
-- V13: One role per account + forced password change on first login
-- ============================================================================

-- 1. Accounts created (or reset) by an admin must change their password on first login
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Collapse existing multi-role accounts to a single role.
--    Keep the most privileged role (ADMIN > LECTURER > STUDENT > USER),
--    preferring the canonical ROLE_* code over its alias (e.g. ROLE_ADMIN over ADMIN).
WITH ranked AS (
    SELECT ur.user_id,
           ur.role_id,
           ROW_NUMBER() OVER (
               PARTITION BY ur.user_id
               ORDER BY CASE REPLACE(r.role_code, 'ROLE_', '')
                            WHEN 'ADMIN' THEN 1
                            WHEN 'LECTURER' THEN 2
                            WHEN 'STUDENT' THEN 3
                            ELSE 4
                        END,
                        CASE WHEN LEFT(r.role_code, 5) = 'ROLE_' THEN 0 ELSE 1 END,
                        ur.role_id
           ) AS rn
    FROM user_roles ur
    JOIN roles r ON r.role_id = ur.role_id
)
DELETE FROM user_roles ur
USING ranked
WHERE ur.user_id = ranked.user_id
  AND ur.role_id = ranked.role_id
  AND ranked.rn > 1;

-- 3. Enforce the rule at the DB level.
--    DEFERRABLE INITIALLY DEFERRED: Hibernate flushes the INSERT of the new role before the
--    orphan DELETE of the old one when an admin changes a user's role; the check runs at commit.
ALTER TABLE user_roles
    ADD CONSTRAINT uq_user_roles_one_role_per_user UNIQUE (user_id) DEFERRABLE INITIALLY DEFERRED;
