INSERT INTO users (email, username, password_hash, cash_balance, is_demo)
VALUES (
    'demo@toptrader.dev',
    'demo_trader',
    '$2a$10$demoAccountPasswordHashIsNeverCheckedByAnyLoginFlow0',
    90.00,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO holdings (user_id, ticker, quantity, average_cost_basis, updated_at)
SELECT id, 'KO', 4, 58.00, TIMESTAMP '2026-03-03 14:32:00'
FROM users WHERE email = 'demo@toptrader.dev'
ON CONFLICT (user_id, ticker) DO NOTHING;

INSERT INTO holdings (user_id, ticker, quantity, average_cost_basis, updated_at)
SELECT id, 'INTC', 3, 32.00, TIMESTAMP '2026-06-22 16:12:00'
FROM users WHERE email = 'demo@toptrader.dev'
ON CONFLICT (user_id, ticker) DO NOTHING;

INSERT INTO holdings (user_id, ticker, quantity, average_cost_basis, updated_at)
SELECT id, 'BAC', 2, 33.00, TIMESTAMP '2026-05-18 09:47:00'
FROM users WHERE email = 'demo@toptrader.dev'
ON CONFLICT (user_id, ticker) DO NOTHING;

INSERT INTO transactions (user_id, ticker, side, quantity, price_per_share, total_amount, executed_at)
SELECT id, 'KO', 'BUY', 4, 58.00, 232.00, TIMESTAMP '2026-03-03 14:32:00'
FROM users WHERE email = 'demo@toptrader.dev';

INSERT INTO transactions (user_id, ticker, side, quantity, price_per_share, total_amount, executed_at)
SELECT id, 'INTC', 'BUY', 5, 32.00, 160.00, TIMESTAMP '2026-04-10 10:05:00'
FROM users WHERE email = 'demo@toptrader.dev';

INSERT INTO transactions (user_id, ticker, side, quantity, price_per_share, total_amount, executed_at)
SELECT id, 'BAC', 'BUY', 2, 33.00, 66.00, TIMESTAMP '2026-05-18 09:47:00'
FROM users WHERE email = 'demo@toptrader.dev';

INSERT INTO transactions (user_id, ticker, side, quantity, price_per_share, total_amount, executed_at)
SELECT id, 'INTC', 'SELL', 2, 24.00, 48.00, TIMESTAMP '2026-06-22 16:12:00'
FROM users WHERE email = 'demo@toptrader.dev';
