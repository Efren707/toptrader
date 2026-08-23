INSERT INTO users (email, username, password_hash, avatar_key, is_demo)
VALUES (
    'jpsullivan82@gmail.com',
    'JP_Sullivan',
    '$2a$10$demoAccountPasswordHashIsNeverCheckedByAnyLoginFlow0',
    'flint',
    FALSE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, username, password_hash, avatar_key, is_demo)
VALUES (
    'mikewazowski47@gmail.com',
    'Mike_Wazowski',
    '$2a$10$demoAccountPasswordHashIsNeverCheckedByAnyLoginFlow0',
    'cosmo',
    FALSE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, username, password_hash, avatar_key, is_demo)
VALUES (
    'randallboggs19@gmail.com',
    'Randall_Boggs',
    '$2a$10$demoAccountPasswordHashIsNeverCheckedByAnyLoginFlow0',
    'willow',
    FALSE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO friendships (requester_id, addressee_id, status, created_at, responded_at)
SELECT seed.id, demo.id, 'ACCEPTED', TIMESTAMP '2026-03-15 10:00:00', TIMESTAMP '2026-03-16 09:00:00'
FROM users seed, users demo
WHERE seed.email = 'jpsullivan82@gmail.com' AND demo.email = 'demo@toptrader.dev'
ON CONFLICT (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) DO NOTHING;

INSERT INTO friendships (requester_id, addressee_id, status, created_at, responded_at)
SELECT seed.id, demo.id, 'ACCEPTED', TIMESTAMP '2026-04-20 14:30:00', TIMESTAMP '2026-04-22 08:15:00'
FROM users seed, users demo
WHERE seed.email = 'mikewazowski47@gmail.com' AND demo.email = 'demo@toptrader.dev'
ON CONFLICT (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) DO NOTHING;

INSERT INTO friendships (requester_id, addressee_id, status, created_at, responded_at)
SELECT seed.id, demo.id, 'ACCEPTED', TIMESTAMP '2026-05-10 17:45:00', TIMESTAMP '2026-05-11 11:20:00'
FROM users seed, users demo
WHERE seed.email = 'randallboggs19@gmail.com' AND demo.email = 'demo@toptrader.dev'
ON CONFLICT (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) DO NOTHING;
