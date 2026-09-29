-- Migration: Add deleted_at column, support DELETED status, and partial unique indexes for active users
-- Compatible with PostgreSQL

-- 1. Add deleted_at column if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- 2. Drop existing CHECK constraint on status if present, and update allowed values
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'users_status_check'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT users_status_check;
        ALTER TABLE users ADD CONSTRAINT users_status_check
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'DELETED'));
    END IF;
END $$;

-- 3. Replace standard unique constraints with partial unique indexes (WHERE deleted_at IS NULL)
-- This allows reusing email or userCode after a record is soft-deleted.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_user_code_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_user_code;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_email ON users(email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_user_code ON users(user_code) WHERE deleted_at IS NULL;
