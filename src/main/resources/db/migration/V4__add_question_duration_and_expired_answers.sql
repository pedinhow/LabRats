ALTER TABLE questions
    ADD COLUMN duration_seconds INTEGER NOT NULL DEFAULT 15;

ALTER TABLE user_answers
    ALTER COLUMN selected_alternative_id DROP NOT NULL;

ALTER TABLE user_answers
    ALTER COLUMN selected_alternative_text DROP NOT NULL;
