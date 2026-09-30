-- V0: Baseline schema — creates all tables from scratch for a fresh database.
-- Flyway runs this before V1 and V2, so ALTER TABLE statements in those migrations
-- are guaranteed to find the tables already existing.

-- ─── ROLES ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    role_id     SERIAL PRIMARY KEY,
    role_code   VARCHAR(30)  NOT NULL UNIQUE,
    role_name   VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── USERS ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    user_code     VARCHAR(50)  NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(20),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE','INACTIVE','BANNED','DELETED')),
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0
);

-- Partial unique indexes: allow reuse of email / user_code after soft-delete
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_email
    ON users(email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_active_user_code
    ON users(user_code) WHERE deleted_at IS NULL;

-- ─── USER_ROLES ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id     UUID        NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    role_id     INTEGER     NOT NULL REFERENCES roles(role_id)  ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID,
    PRIMARY KEY (user_id, role_id)
);
