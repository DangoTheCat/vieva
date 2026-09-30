-- Migration V2: Add optimistic locking version column and enforce partial unique indexes
-- These changes support:
--   1. @Version field on UserJpaEntity for optimistic locking (race condition protection)
--   2. Partial unique indexes for soft-delete email/userCode deduplication

-- 1. Add version column for optimistic locking (@Version in JPA)
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Ensure partial unique indexes exist (idempotent -- IF NOT EXISTS)
-- These allow the same email/userCode after soft-delete while enforcing uniqueness for active users.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_email
    ON users(email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_user_code
    ON users(user_code) WHERE deleted_at IS NULL;
