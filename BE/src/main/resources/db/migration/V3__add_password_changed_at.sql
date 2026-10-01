-- Migration V3: Add password_changed_at column for JWT revocation on password change
-- When a user changes their password, password_changed_at is updated.
-- Any JWT issued before this timestamp is considered revoked by JwtAuthenticationFilter.

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP WITH TIME ZONE;
