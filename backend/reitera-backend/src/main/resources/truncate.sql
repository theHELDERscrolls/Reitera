-- ============================================================
-- Reitera — Database Truncation (Depopulation)
-- ============================================================
-- Deletes ALL rows from every table WITHOUT dropping the tables.
-- The schema (columns, constraints, indexes) is preserved intact.
--
-- Use this to:
--   - Reset the DB before running seed.sql again
--   - Start fresh without recreating the schema
--
-- CASCADE propagates the truncation to dependent tables
-- automatically, so order does not matter here.
-- Sequences (SERIAL auto-increment counters) are also reset
-- to 1 so IDs start from 1 again on the next insert.
-- ============================================================

TRUNCATE TABLE
    refresh_tokens,
    review_logs,
    study_progress,
    cards,
    notes,
    decks,
    categories,
    users,
    roles
RESTART IDENTITY CASCADE;
