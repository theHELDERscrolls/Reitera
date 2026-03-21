# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — feature/09
### Planned
- Deck import / export (JSON format)

---

## [0.8.0] — 2026-03-21 — feature/08-tags-user-swagger
### Added
- `GET /api/v1/users/me` — returns the authenticated user's own profile (id, username, email, firstName, lastName, roleName)
- `GET /api/v1/categories` — returns the distinct categories used across the authenticated user's decks (for frontend autocomplete)
- `GET /api/v1/tags` — returns the distinct tags assigned to cards in the authenticated user's decks
- `GET /api/v1/tags/{tagId}/cards` — returns the user's cards filtered by tag; always returns 200 to prevent tag ID enumeration
- `UserController` at `/api/v1/users`
- `CategoryController` and `CategoryService` at `/api/v1/categories`
- `TagController` and `TagService` at `/api/v1/tags`
- `CategoryResponseDTO` and `TagResponseDTO`
- Postman collection updated with Users, Categories and Tags folders; Login split into alumno / admin variants; CLOZE example corrected to current format

### Changed
- `DeckService.createDeck()` — `authorName` now uses `username` instead of `firstName + lastName` to ensure uniqueness
- `seed.sql` — `author_name` updated to `'alumno'` across all 4 demo decks
- `init.sql` — `DOUBLE PRECISION` replaced with `FLOAT8` alias (functionally identical; fixes DBeaver parser warning)

---

## [0.7.0] — 2026-03-19 — feature/07-seed-data
### Added
- `backend/src/main/resources/init.sql` — full database schema (DDL) for all 10 tables
  - Enables `pgcrypto` extension for BCrypt password hashing via `seed.sql`
  - Uses `CREATE TABLE IF NOT EXISTS` to be safe on repeated runs
- `backend/src/main/resources/seed.sql` — idempotent demo data script
  - 2 demo users with BCrypt-hashed passwords generated at runtime via `crypt()` + `gen_salt('bf', 10)`
  - 3 categories, 4 tags, 4 decks (two under "Historia de España" to enable category-scoped study demo)
  - 21 cards covering all 4 types: `BASIC`, `CLOZE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`
  - 6 `study_progress` records for Deck 1 with all FSRS states: Learning, Review, Relearning
  - 14 `review_log` entries simulating realistic review history
  - Idempotency guard: raises error if seed data already exists
- `docs/setup/database-setup.md` updated with sections for `init.sql` and `seed.sql`

---

## [0.6.0] — 2026-03-18 — feature/06-fsrs-study-session
### Added
- `FsrsService` — pure Java implementation of the FSRS-6 spaced repetition algorithm
  - Initial stability `S₀` and difficulty `D₀` for new cards
  - Stability update after recall (`S'_recall`) and after forgetting (`S'_forget`)
  - Forgetting curve `R(t, S)` with minute-precision intervals (sub-day reviews supported)
  - State machine: New → Learning → Review ↔ Relearning
- `StudyService` — orchestrates DB access, calls `FsrsService`, persists results in a single `@Transactional` batch
- `StudyController` — two REST endpoints:
  - `GET /api/v1/study/due?deckId={id}` — cards due for review in a single deck
  - `GET /api/v1/study/due?categoryId={id}` — cards due across all decks in a category
  - `POST /api/v1/study/sessions` — submits all ratings from a completed session
- `StudyProgressRepository.findDueByUserAndDeck` and `findDueByUserAndCategory` JPQL queries
- `CardRepository.findNewCardsByDeckAndUser` and `findNewCardsByCategoryAndUser` JPQL queries
- DTOs: `CardRatingDTO`, `StudySessionRequestDTO`, `StudySessionResponseDTO`, `DueCardDTO`
- `docs/core-logic/fsrs-algorithm.md` — full algorithm reference document
- Postman collection updated with Study folder (4 requests)

---

## [0.5.0] — 2026-03-18 — feature/05-deck-card-crud
### Added
- `DeckService` and `DeckController` — full CRUD for decks (`GET`, `POST`, `PUT`, `DELETE`)
- `CardService` and `CardController` — full CRUD for cards, nested under `/decks/{id}/cards`
- `CardRequestDTO` and `CardResponseDTO` with nested `TagSummary` projection
- `ResourceNotFoundException` — returns HTTP 404 when a Deck, Card or Category is not found
- `GlobalExceptionHandler` updated to handle 404 separately from generic 400 errors
- `DeckRepository.findAllByOwner(User)` and `CardRepository.findAllByDeck(Deck)` queries
- Category and Tag resolution during Deck/Card creation
- Ownership enforcement: users can only modify their own decks and cards
- Postman collection at `docs/api/reitera-postman-collection.json`

---

## [0.4.0] — feature/04-content-domain
### Added
- `Card` entity with dynamic `JSONB` answer storage (`answer_json` column)
  - Supported types: `BASIC`, `CLOZE`, `MULTIPLE_CHOICE`
- `Deck` entity with ownership (`owner_id`) and optional category
- `Category` and `Tag` entities for deck/card organization
- `UserDeckSubscription` entity with composite key for public deck subscriptions
- `StudyProgress` entity with composite key `(user_id, card_id)` — stores FSRS state variables
- `ReviewLog` entity — immutable audit log of every study review action
- All corresponding Spring Data JPA repositories (11 total)

---

## [0.3.0] — feature/03-login-jwt
### Added
- `JwtService` — JWT generation and validation (HMAC-SHA, 24h expiry)
- `JwtAuthenticationFilter` — per-request token validation, populates `SecurityContext`
- `SecurityConfig` — stateless session policy, public auth routes, protected everything else
- `ApplicationConfig` — `UserDetailsService`, `AuthenticationManager`, `BCryptPasswordEncoder` beans
- `POST /api/v1/auth/login` endpoint — returns signed JWT on valid credentials

---

## [0.2.0] — feature/02-user-entities
### Added
- `User` entity with UUID primary key, implementing Spring Security `UserDetails`
- `Role` entity — `STUDENT` and `ADMIN` roles
- `UserRegisterDTO` with full validation (username length, email format, password strength)
- `UserResponseDTO` — safe projection without password
- `UserService.registerUser()` — uniqueness checks, BCrypt hashing, role assignment
- `POST /api/v1/auth/register` endpoint
- `GlobalExceptionHandler` — centralized error handling for validation and runtime errors

---

## [0.1.0] — feature/01-project-setup
### Added
- Spring Boot 4.0.3 project scaffold (Java 21, Maven)
- `docker-compose.yml` with PostgreSQL 16 service
- Monorepo structure: `backend/`, `frontend/`, `docs/`
- GitHub issue and PR templates (`bug_report.md`, `feature_request.md`, `PULL_REQUEST_TEMPLATE.md`)
- `docs/setup/database-setup.md` — local database setup guide
