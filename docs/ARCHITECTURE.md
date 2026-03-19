# Architecture Overview

This document describes the high-level architecture of the Reitera backend and the main design decisions taken during development.

## System Architecture

Reitera follows a **monorepo structure** with two independent applications:

```
Reitera/
├── backend/    → Spring Boot REST API (Java 21)
├── frontend/   → Angular SPA (TypeScript)
└── docs/       → Project documentation
```

The backend exposes a stateless REST API consumed by the Angular frontend. There is no server-side rendering.

## Backend Layer Architecture

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
├── auth/           → JWT filter, JwtService, public login/register endpoints
├── user/           → User & Role entities, registration, authentication logic
├── deck/           → Deck, Category, Tag, UserDeckSubscription entities + CRUD API
├── card/           → Card entity + CRUD API (nested under decks)
└── study/          → StudyProgress, ReviewLog entities + FSRS-6 algorithm + study session API

core/
└── exception/      → GlobalExceptionHandler, ResourceNotFoundException
```

## Security Model

Authentication is **stateless JWT-based**:

1. User logs in → server issues a signed JWT (24h expiry, HMAC-SHA)
2. Client sends `Authorization: Bearer <token>` on every request
3. `JwtAuthenticationFilter` validates the token and populates the `SecurityContext`
4. Controllers receive the authenticated `User` via `@AuthenticationPrincipal`

Public endpoints (no token required): `POST /api/v1/auth/register`, `POST /api/v1/auth/login`
All other endpoints are protected.

## Database Design

- **Schema management:** Manual SQL scripts (`ddl-auto: none`). No Flyway/Liquibase yet.
- **Primary keys:** UUID for `users`, auto-increment Integer for all other entities.
- **Flexible card answers:** `answer_json` is stored as native PostgreSQL `JSONB`, mapped via Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` to a `Map<String, Object>`. This allows different card types (BASIC, CLOZE, MULTIPLE_CHOICE) to use different answer structures without schema changes.
- **Composite keys:** `StudyProgress` uses a composite PK of `(user_id, card_id)` — one progress record per user per card.

## Study Session Flow (design decision)

Study sessions follow a **batch architecture** — the backend is only hit twice per session:

1. Frontend fetches due cards: `GET /api/v1/study/due?deckId={id}` (or `?categoryId={id}`)
   - Returns overdue cards (previously studied, `nextReview ≤ now`) + new cards (no prior progress)
   - Cards carry their FSRS `state` field so the UI can show New / Learning / Review badges
2. User studies the session locally — ratings held in memory (or `localStorage` for resilience)
3. On session completion, a **single batch request** submits all ratings: `POST /api/v1/study/sessions`
4. Backend runs FSRS-6 for each card in one `@Transactional` block and persists `StudyProgress` + `ReviewLog`

**Scope modes:** both endpoints accept either `deckId` (single deck) or `categoryId` (all decks in a category), enabling users to study individual topics or full subjects at once.

**Interval precision:** intervals are stored and applied at minute precision. Low-stability cards (e.g. Again on a new card) receive sub-day intervals (~5 hours) rather than being forced to the next day.
