-- ============================================================
-- Reitera — Database Schema Initialization
-- ============================================================
-- Safe to re-run in development: drops all tables first so schema
-- changes (new columns, new constraints) are always applied cleanly.
-- Prerequisites: Docker container 'reitera_postgres' running,
-- connected to 'reitera_db' as 'reitera_admin'.
-- ============================================================

-- Required by seed.sql to generate BCrypt password hashes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Drop all tables in reverse dependency order so FK constraints don't block the drops.
-- CASCADE is not needed here because we respect the dependency order,
-- but it is kept as a safety net.
DROP TABLE IF EXISTS refresh_tokens         CASCADE;
DROP TABLE IF EXISTS review_logs            CASCADE;
DROP TABLE IF EXISTS study_progress         CASCADE;
DROP TABLE IF EXISTS card_tags              CASCADE;
DROP TABLE IF EXISTS user_deck_subscriptions CASCADE;
DROP TABLE IF EXISTS cards                  CASCADE;
DROP TABLE IF EXISTS decks                  CASCADE;
DROP TABLE IF EXISTS tags                   CASCADE;
DROP TABLE IF EXISTS categories             CASCADE;
DROP TABLE IF EXISTS users                  CASCADE;
DROP TABLE IF EXISTS roles                  CASCADE;


-- 1. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL      PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);


-- 2. USERS
CREATE TABLE IF NOT EXISTS users (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR      NOT NULL,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    role_id    INTEGER      NOT NULL REFERENCES roles(id)
);


-- 3. CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL      PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP   DEFAULT NOW()
);


-- 4. TAGS
-- owner_id scopes tags to a user — two users can have the same tag name independently.
-- The unique constraint is (name, owner_id), not just name.
CREATE TABLE IF NOT EXISTS tags (
    id        SERIAL      PRIMARY KEY,
    name      VARCHAR(30) NOT NULL,
    hex_color VARCHAR(7),
    owner_id  UUID        NOT NULL REFERENCES users(id),
    UNIQUE (name, owner_id)
);


-- 5. DECKS
CREATE TABLE IF NOT EXISTS decks (
    id          SERIAL       PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    owner_id    UUID         NOT NULL REFERENCES users(id),
    author_name VARCHAR(150),
    category_id INTEGER      REFERENCES categories(id),
    created_at  TIMESTAMP    DEFAULT NOW(),
    updated_at  TIMESTAMP    DEFAULT NOW()
);


-- 6. CARDS
-- answer_json uses JSONB to support multiple card types with different answer structures.
-- See docs/api/endpoints.md for the format per type (BASIC, CLOZE, MULTIPLE_CHOICE, TRUE_FALSE).
CREATE TABLE IF NOT EXISTS cards (
    id          SERIAL      PRIMARY KEY,
    deck_id     INTEGER     NOT NULL REFERENCES decks(id),
    type        VARCHAR(50) NOT NULL,
    question    TEXT        NOT NULL,
    answer_json JSONB       NOT NULL,
    explanation TEXT
);


-- 7. CARD_TAGS (many-to-many: cards ↔ tags)
CREATE TABLE IF NOT EXISTS card_tags (
    card_id INTEGER NOT NULL REFERENCES cards(id),
    tag_id  INTEGER NOT NULL REFERENCES tags(id),
    PRIMARY KEY (card_id, tag_id)
);


-- 8. STUDY_PROGRESS
-- One row per (user, card) pair. Stores FSRS state variables.
-- States: 0=New, 1=Learning, 2=Review, 3=Relearning.
CREATE TABLE IF NOT EXISTS study_progress (
    user_id        UUID             NOT NULL REFERENCES users(id),
    card_id        INTEGER          NOT NULL REFERENCES cards(id),
    stability      FLOAT8           NOT NULL DEFAULT 0.0,
    difficulty     FLOAT8           NOT NULL DEFAULT 0.0,
    elapsed_days   INTEGER          NOT NULL DEFAULT 0,
    scheduled_days INTEGER          NOT NULL DEFAULT 0,
    reps           INTEGER          NOT NULL DEFAULT 0,
    lapses         INTEGER          NOT NULL DEFAULT 0,
    state          INTEGER          NOT NULL DEFAULT 0,
    last_review    TIMESTAMP,
    next_review    TIMESTAMP,
    PRIMARY KEY (user_id, card_id)
);


-- 9. REVIEW_LOGS
-- Immutable audit ledger. One row per review action. Never updated, only appended.
CREATE TABLE IF NOT EXISTS review_logs (
    id             SERIAL  PRIMARY KEY,
    user_id        UUID    NOT NULL REFERENCES users(id),
    card_id        INTEGER NOT NULL REFERENCES cards(id),
    rating         INTEGER NOT NULL,
    elapsed_days   INTEGER NOT NULL,
    scheduled_days INTEGER NOT NULL,
    review_date    TIMESTAMP DEFAULT NOW()
);


-- 10. USER_DECK_SUBSCRIPTIONS
CREATE TABLE IF NOT EXISTS user_deck_subscriptions (
    user_id       UUID    NOT NULL REFERENCES users(id),
    deck_id       INTEGER NOT NULL REFERENCES decks(id),
    subscribed_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, deck_id)
);


-- 11. REFRESH_TOKENS
-- One row per active session. Stores a SHA-256 hash of the raw token (never the token itself).
-- The revoked flag allows explicit logout without deleting the row, preserving the audit trail.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         SERIAL       PRIMARY KEY,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    DEFAULT NOW()
);


-- INDEXES
-- PostgreSQL does not auto-index FK columns. These cover the most frequent query paths.
-- See docs/ARCHITECTURE.md for the rationale behind each index.

CREATE INDEX IF NOT EXISTS idx_study_progress_user_next_review
    ON study_progress (user_id, next_review);

CREATE INDEX IF NOT EXISTS idx_cards_deck_id
    ON cards (deck_id);

CREATE INDEX IF NOT EXISTS idx_decks_owner_id
    ON decks (owner_id);

CREATE INDEX IF NOT EXISTS idx_review_logs_user_id
    ON review_logs (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
