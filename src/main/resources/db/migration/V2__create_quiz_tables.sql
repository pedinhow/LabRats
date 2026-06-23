CREATE TABLE quizzes (
    id UUID PRIMARY KEY NOT NULL,
    title VARCHAR(255) NOT NULL,
    creator_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_quiz_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE questions (
    id UUID PRIMARY KEY NOT NULL,
    statement TEXT NOT NULL,
    quiz_id UUID NOT NULL,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE alternatives (
    id UUID PRIMARY KEY NOT NULL,
    text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    question_id UUID NOT NULL,
    CONSTRAINT fk_alternative_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_quizzes_creator_id ON quizzes(creator_id);
CREATE INDEX idx_questions_quiz_id ON questions(quiz_id);
CREATE INDEX idx_alternatives_question_id ON alternatives(question_id);
