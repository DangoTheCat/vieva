-- V17: Add avatar_url column to users table for Cloudinary avatar storage
BEGIN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
COMMIT;
