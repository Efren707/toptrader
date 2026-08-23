CREATE TABLE friendships (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requester_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    responded_at  TIMESTAMP,
    CONSTRAINT chk_friendships_not_self CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX uq_friendships_pair ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));