CREATE TABLE email_verification_tokens (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    token_hash      VARCHAR(64)     NOT NULL,
    expires_at      TIMESTAMP       NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_email_verification_token FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_email_verification_tokens_token_hash UNIQUE (token_hash)
);

ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP NULL;
UPDATE users SET email_verified_at = NOW() WHERE email_verified_at IS NULL;
