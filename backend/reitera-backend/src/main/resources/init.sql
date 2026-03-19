-- ============================================================
-- Reitera — Database Schema Initialization
-- ============================================================
-- Run this script ONCE when setting up a new local environment.
-- It creates all tables, constraints, and indexes for the app.
--
-- Prerequisites:
--   - Docker container 'reitera_postgres' is running.
--   - Connected to database 'reitera_db' as user 'reitera_admin'.
--
-- Table order matters: referenced tables must exist before
-- tables that reference them (foreign key constraint order).
-- ============================================================

-- pgcrypto is used by seed.sql to generate BCrypt password hashes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- =====================================f=======================
-- 1. ROLES
-- Simple lookup table for RBAC (Role-Based Access Control).
-- Values: USER, ADMIN
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL      PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);


-- ============================================================
-- 2. USERS
-- Core auth entity. UUID primary key (not SERIAL) to prevent
-- enumeration attacks and allow distributed ID generation.
-- References roles(id) — role is mandatory for every user.
-- ============================================================
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


-- ============================================================
-- 3. CATEGORIES
-- High-level grouping for decks (e.g. "Historia de España").
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL      PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP   DEFAULT NOW()
);


-- ============================================================
-- 4. TAGS
-- Labels that can be attached to cards for cross-deck grouping.
-- hex_color stores a CSS hex color (e.g. "#E74C3C").
-- ============================================================
CREATE TABLE IF NOT EXISTS tags (
    id        SERIAL      PRIMARY KEY,
    name      VARCHAR(30) NOT NULL UNIQUE,
    hex_color VARCHAR(7)
);


-- ============================================================
-- 5. DECKS
-- A collection of flashcards owned by a user.
-- category_id is optional (nullable FK).
-- is_public controls community sharing visibility.
-- ============================================================
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


-- ============================================================
-- 6. CARDS
-- A single flashcard belonging to a deck.
-- answer_json uses JSONB (binary JSON) to support multiple
-- card types with different answer structures:
--   BASIC:           {"answer": "text"}
--   CLOZE:           {"answer": "hidden word"}
--   MULTIPLE_CHOICE: {"options": [...], "correctIndex": N}
--   TRUE_FALSE:      {"answer": true|false}
-- ============================================================
CREATE TABLE IF NOT EXISTS cards (
    id          SERIAL      PRIMARY KEY,
    deck_id     INTEGER     NOT NULL REFERENCES decks(id),
    type        VARCHAR(50) NOT NULL,
    question    TEXT        NOT NULL,
    answer_json JSONB       NOT NULL,
    explanation TEXT
);


-- ============================================================
-- 7. CARD_TAGS
-- Intermediate table for the many-to-many between cards and tags.
-- Managed automatically by Hibernate (@JoinTable).
-- ============================================================
CREATE TABLE IF NOT EXISTS card_tags (
    card_id INTEGER NOT NULL REFERENCES cards(id),
    tag_id  INTEGER NOT NULL REFERENCES tags(id),
    PRIMARY KEY (card_id, tag_id)
);


-- ============================================================
-- 8. STUDY_PROGRESS
-- The heart of the FSRS algorithm. One row per (user, card) pair.
-- Tracks stability, difficulty, state and scheduling dates.
--
-- FSRS states:
--   0 = New       (never studied)
--   1 = Learning  (first encounters, short intervals)
--   2 = Review    (long-term memory, days/weeks apart)
--   3 = Relearning (forgot a Review card, starting again)
-- ============================================================
CREATE TABLE IF NOT EXISTS study_progress (
    user_id        UUID             NOT NULL REFERENCES users(id),
    card_id        INTEGER          NOT NULL REFERENCES cards(id),
    stability      DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    difficulty     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    elapsed_days   INTEGER          NOT NULL DEFAULT 0,
    scheduled_days INTEGER          NOT NULL DEFAULT 0,
    reps           INTEGER          NOT NULL DEFAULT 0,
    lapses         INTEGER          NOT NULL DEFAULT 0,
    state          INTEGER          NOT NULL DEFAULT 0,
    last_review    TIMESTAMP,
    next_review    TIMESTAMP,
    PRIMARY KEY (user_id, card_id)
);


-- ============================================================
-- 9. REVIEW_LOGS
-- Immutable audit ledger. One row per review action.
-- Never updated, only appended. Used for FSRS optimization
-- and user analytics (heatmaps, session history, etc.).
-- ============================================================
CREATE TABLE IF NOT EXISTS review_logs (
    id             SERIAL  PRIMARY KEY,
    user_id        UUID    NOT NULL REFERENCES users(id),
    card_id        INTEGER NOT NULL REFERENCES cards(id),
    rating         INTEGER NOT NULL,
    elapsed_days   INTEGER NOT NULL,
    scheduled_days INTEGER NOT NULL,
    review_date    TIMESTAMP DEFAULT NOW()
);


-- ============================================================
-- 10. USER_DECK_SUBSCRIPTIONS
-- Tracks which users have subscribed to public decks
-- from the community. Composite PK (user_id, deck_id).
-- ============================================================
CREATE TABLE IF NOT EXISTS user_deck_subscriptions (
    user_id       UUID    NOT NULL REFERENCES users(id),
    deck_id       INTEGER NOT NULL REFERENCES decks(id),
    subscribed_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, deck_id)
);


-- ============================================================
-- INDEXES
-- PostgreSQL automatically creates indexes for PRIMARY KEY and
-- UNIQUE constraints. The indexes below cover the additional
-- access patterns that the application queries frequently.
--
-- Without these, PostgreSQL performs a sequential scan (reads
-- every row in the table) for each query — acceptable at small
-- scale, but progressively slower as data grows.
-- ============================================================

-- study_progress: the hot path for every study session.
-- Filters by user_id and next_review on every GET /study/due call.
-- The composite index (user_id, next_review) lets PostgreSQL jump
-- directly to the rows for a specific user whose review is overdue.
CREATE INDEX IF NOT EXISTS idx_study_progress_user_next_review
    ON study_progress (user_id, next_review);

-- cards: fetching all cards for a deck is the most common card query.
-- FK columns are not automatically indexed in PostgreSQL — this
-- makes GET /decks/{id}/cards fast regardless of table size.
CREATE INDEX IF NOT EXISTS idx_cards_deck_id
    ON cards (deck_id);

-- decks: listing all decks owned by a user (GET /decks endpoint).
CREATE INDEX IF NOT EXISTS idx_decks_owner_id
    ON decks (owner_id);

-- review_logs: analytics queries filter by user to build
-- heatmaps and session history (future dashboard feature).
CREATE INDEX IF NOT EXISTS idx_review_logs_user_id
    ON review_logs (user_id);
