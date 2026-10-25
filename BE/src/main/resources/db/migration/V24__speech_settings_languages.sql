-- V24: Add explicit language and voice configuration to speech_settings table

-- 1. Add columns nullable first without defaults so existing rows have NULL
ALTER TABLE speech_settings
    ADD COLUMN IF NOT EXISTS stt_language VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tts_language VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vietnamese_voice VARCHAR(100),
    ADD COLUMN IF NOT EXISTS english_voice VARCHAR(100);

-- 2. Backfill existing rows from legacy columns, preserving any custom voice/locale
UPDATE speech_settings
SET
    stt_language = COALESCE(NULLIF(stt_language, ''), locale, 'vi-VN'),
    tts_language = COALESCE(NULLIF(tts_language, ''), locale, 'vi-VN'),
    vietnamese_voice = COALESCE(NULLIF(vietnamese_voice, ''), voice, 'maichi')
WHERE id IS NOT NULL;

-- 3. Set column defaults and NOT NULL constraints
ALTER TABLE speech_settings
    ALTER COLUMN stt_language SET DEFAULT 'vi-VN',
    ALTER COLUMN stt_language SET NOT NULL,
    ALTER COLUMN tts_language SET DEFAULT 'vi-VN',
    ALTER COLUMN tts_language SET NOT NULL,
    ALTER COLUMN vietnamese_voice SET DEFAULT 'maichi',
    ALTER COLUMN vietnamese_voice SET NOT NULL;
