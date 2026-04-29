# Backend Architecture

## Layer Architecture

The backend follows a standard **3-tier N-layer architecture**:

```
HTTP Request
     │
     ▼
┌─────────────┐
│  Controller │  Receives HTTP requests, delegates to service, returns ResponseEntity
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │  Business logic, ownership checks, DTO ↔ Entity mapping
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  Spring Data JPA — translates method names into SQL queries
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  PostgreSQL │  Persistent storage (Docker in local, managed service in production)
└─────────────┘
```

Data flows **inward** as DTOs and is **mapped to entities** only at the service layer. Entities never leave the service layer — controllers always deal with DTOs.

## Module Structure

The backend is organized by **feature module**, not by technical layer:

```
modules/
├── auth/
│   ├── controller/ → AuthController: /register, /login, /refresh, /logout
│   ├── filter/     → JwtAuthenticationFilter (intercepts every request)
│   ├── model/      → RefreshToken entity
│   ├── repository/ → RefreshTokenRepository
│   └── service/    → JwtService, RefreshTokenService
├── user/           → User & Role entities, registration, authentication logic, profile endpoint (UserController)
├── deck/           → Deck, Category, Tag, UserDeckSubscription entities + CRUD, categories, tags and deck stats APIs
├── card/           → Card entity + CRUD API (nested under decks) + tag-filtered search + inline tag find-or-create
├── study/          → StudyProgress, ReviewLog entities + FSRS-6 algorithm + study session API
└── dashboard/      → DashboardController, DashboardService — stats, heatmap and last-studied endpoints

core/
└── exception/      → GlobalExceptionHandler, ResourceNotFoundException
```

**Tag lifecycle:** Tags are user-scoped — the uniqueness constraint is `UNIQUE(name, owner_id)`, not global. Tags are created inline during card save via `TagService.findOrCreateTag()` (same find-or-create pattern as categories). Tags with no remaining cards are automatically deleted by `CardService.cleanupOrphanTags()` after every update or delete, inside the same `@Transactional` block.

## Database Design

- **Schema management:** Manual SQL scripts (`ddl-auto: none`). No Flyway/Liquibase yet.
- **Primary keys:** UUID for `users`, auto-increment Integer for all other entities.
- **Flexible card answers:** `answer_json` is stored as native PostgreSQL `JSONB`, mapped via Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` to a `Map<String, Object>`. This allows different card types (BASIC, MULTIPLE_CHOICE, TRUE_FALSE) to use different answer structures without schema changes.
- **Composite keys:** `StudyProgress` uses a composite PK of `(user_id, card_id)` — one progress record per user per card.
- **Refresh tokens:** the raw UUID token is never stored. Only its SHA-256 hex digest (`token_hash VARCHAR(64)`) is persisted. `ON DELETE CASCADE` on `user_id` ensures cleanup on user deletion. The `revoked` flag preserves the audit trail without physically deleting rows.

## Study Session Flow

Study sessions follow a **batch architecture** — the backend is only hit twice per session:

1. Frontend fetches due cards: `GET /api/v1/study/due?deckId={id}` (or `?categoryId={id}`)
   - Returns overdue cards (previously studied, `nextReview ≤ now`) + new cards (no prior progress)
   - Cards carry their FSRS `state` field for client-side processing
2. User studies the session locally — ratings held in memory (or `localStorage` for resilience)
3. On session completion, a **single batch request** submits all ratings: `POST /api/v1/study/sessions`
4. Backend runs FSRS-6 for each card in one `@Transactional` block and persists `StudyProgress` + `ReviewLog`

**Scope modes:** both endpoints accept either `deckId` (single deck) or `categoryId` (all decks in a category), enabling users to study individual topics or full subjects at once.

**Interval precision:** intervals are stored and applied at minute precision. Low-stability cards (e.g. Again on a new card) receive sub-day intervals (~5 hours) rather than being forced to the next day.
