CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY NOT NULL,
    quiz_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    score INTEGER,
    total_questions INTEGER NOT NULL,
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_participant FOREIGN KEY (participant_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_answers (
    id UUID PRIMARY KEY NOT NULL,
    attempt_id UUID NOT NULL,
    question_id UUID NOT NULL,
    question_statement TEXT NOT NULL,
    selected_alternative_id UUID NOT NULL,
    selected_alternative_text TEXT NOT NULL,
    correct_alternative_id UUID NOT NULL,
    correct_alternative_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    CONSTRAINT fk_user_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_attempts_participant_id ON quiz_attempts(participant_id);
CREATE INDEX idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);
CREATE INDEX idx_user_answers_attempt_id ON user_answers(attempt_id);
