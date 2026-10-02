-- One topic name per subject (case-insensitive).
-- Guards concurrent createTopic / import races that pass the application-level
-- duplicate check simultaneously: the loser hits this index.
CREATE UNIQUE INDEX IF NOT EXISTS uq_topics_subject_lower_name
    ON topics (subject_id, lower(topic_name));
