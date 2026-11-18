CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    grade         VARCHAR(50)  NOT NULL DEFAULT '',
    target_role   VARCHAR(100) NOT NULL DEFAULT '',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
