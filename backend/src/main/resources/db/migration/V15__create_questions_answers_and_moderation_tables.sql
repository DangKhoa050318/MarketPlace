-- V15: Product questions, answers, content helpful votes, and moderation audit logs.

CREATE TABLE product_questions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    helpful_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_product_status ON product_questions(product_id, status, created_at DESC);
CREATE INDEX idx_questions_user_id ON product_questions(user_id);
CREATE INDEX idx_questions_status ON product_questions(status);

CREATE TABLE product_answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES product_questions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    helpful_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_answers_question_status ON product_answers(question_id, status, is_official DESC, created_at ASC);
CREATE INDEX idx_answers_user_id ON product_answers(user_id);
CREATE INDEX idx_answers_status ON product_answers(status);

CREATE TABLE content_helpful_votes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_target UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_helpful_votes_user ON content_helpful_votes(user_id);
CREATE INDEX idx_helpful_votes_target ON content_helpful_votes(target_type, target_id);

CREATE TABLE moderation_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL REFERENCES users(id),
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_moderation_target ON moderation_audit_logs(target_type, target_id, created_at DESC);
CREATE INDEX idx_moderation_moderator ON moderation_audit_logs(moderator_id);
