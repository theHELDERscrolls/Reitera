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
├── auth/
│   ├── controller/ → AuthController: /register, /login, /refresh, /logout
│   ├── filter/     → JwtAuthenticationFilter (intercepts every request)
│   ├── model/      → RefreshToken entity
│   ├── repository/ → RefreshTokenRepository
│   └── service/    → JwtService, RefreshTokenService
├── user/           → User & Role entities, registration, authentication logic, profile endpoint (UserController)
├── deck/           → Deck, Category, Tag, UserDeckSubscription entities + CRUD, categories, tags and deck stats APIs
├── card/           → Card entity + CRUD API (nested under decks) + tag-filtered search + inline tag find-or-create
└── study/          → StudyProgress, ReviewLog entities + FSRS-6 algorithm + study session API

core/
└── exception/      → GlobalExceptionHandler, ResourceNotFoundException
```

**Tag lifecycle:** Tags are user-scoped — the uniqueness constraint is `UNIQUE(name, owner_id)`, not global. Tags are created inline during card save via `TagService.findOrCreateTag()` (same find-or-create pattern as categories). Tags with no remaining cards are automatically deleted by `CardService.cleanupOrphanTags()` after every update or delete, inside the same `@Transactional` block.

## Security Model

Authentication uses a **two-token stateless strategy**:

| Token | Type | Expiry | Storage |
|---|---|---|---|
| Access token | Signed JWT (HMAC-SHA) | 15 minutes | `localStorage` |
| Refresh token | Random UUID (SHA-256 hashed) | 7 days | `refresh_tokens` table |

**Flow:**
1. `POST /auth/login` → server issues both tokens; refresh token hash persisted in DB
2. Client sends `Authorization: Bearer <accessToken>` on every request
3. `JwtAuthenticationFilter` validates the JWT and populates the `SecurityContext`; if the token is missing, malformed, or expired (`JwtException`) the filter skips authentication and continues the chain — Spring Security then returns `401 Unauthorized` via the configured `AuthenticationEntryPoint`
4. Controllers receive the authenticated `User` via `@AuthenticationPrincipal`
5. When the access token expires, `jwtInterceptor` intercepts the `401`, calls `POST /auth/refresh` transparently, persists the new token pair, and retries the original request — the user never notices the renewal
6. If the refresh token is also expired or revoked, `AuthService.logout()` is called and the user is redirected to the login page
7. `POST /auth/logout` → server revokes all active refresh tokens for the user

Public endpoints (no token required): `/register`, `/login`, `/refresh`, `/logout`
All other endpoints are protected.

**Refresh token rotation:** each refresh token is single-use. After being exchanged for a new pair, it is immediately marked `revoked = true`. This limits the damage window if a token is stolen — once used, the old token is worthless.

## Database Design

- **Schema management:** Manual SQL scripts (`ddl-auto: none`). No Flyway/Liquibase yet.
- **Primary keys:** UUID for `users`, auto-increment Integer for all other entities.
- **Flexible card answers:** `answer_json` is stored as native PostgreSQL `JSONB`, mapped via Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` to a `Map<String, Object>`. This allows different card types (BASIC, CLOZE, MULTIPLE_CHOICE) to use different answer structures without schema changes.
- **Composite keys:** `StudyProgress` uses a composite PK of `(user_id, card_id)` — one progress record per user per card.
- **Refresh tokens:** the raw UUID token is never stored. Only its SHA-256 hex digest (`token_hash VARCHAR(64)`) is persisted. `ON DELETE CASCADE` on `user_id` ensures cleanup on user deletion. The `revoked` flag preserves the audit trail without physically deleting rows.

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

## Frontend Architecture

The Angular 21 frontend is a **Single-Page Application** using standalone components — no NgModules anywhere.

### Folder Structure

```
src/app/
├── core/               → Singleton services, guards, interceptors, and models (never imported by feature modules)
│   ├── auth/           → AuthService (login, register, logout, GET /users/me, token storage)
│   ├── guards/         → authGuard, noAuthGuard (functional CanActivateFn)
│   ├── interceptors/   → jwtInterceptor (attaches Bearer token; intercepts 401 to refresh and retry)
│   ├── models/         → TypeScript interfaces mapping all backend DTOs
│   ├── profile-panel/  → ProfilePanelService (stub — scaffolded for future use)
│   ├── theme/          → ThemeService (dark/light, localStorage persistence, data-theme on <html>)
│   └── toast/          → ToastService
├── features/           → One folder per feature; each has its own routes file and components
│   ├── auth/           → auth.routes.ts, LoginComponent, RegisterComponent
│   ├── dashboard/      → DashboardComponent (stub)
│   ├── decks/          → DecksComponent — paginated deck list with category filter and CRUD dialogs
│   │   ├── components/
│   │   │   ├── card-form/         → CardFormComponent (create/edit card dialog; tag combobox; dynamic MC options)
│   │   │   ├── cards-table/       → CardsTableComponent (sortable table; loading skeleton; empty state; reusable)
│   │   │   ├── category-filter/   → CategoryFilterComponent (collapsible dropdown, click-outside aware)
│   │   │   ├── deck-card/         → DeckCardComponent (accent color, options menu, edit/delete outputs)
│   │   │   ├── deck-detail/       → DeckDetailComponent (breadcrumb, header, stats chips, cards table, pagination)
│   │   │   ├── deck-form/         → DeckFormComponent (create/edit dialog, reactive form, save confirm)
│   │   │   └── visibility-badge/  → VisibilityBadgeComponent (public/private pill; used in deck-card and deck-detail)
│   │   └── services/
│   │       ├── deck-detail.service.ts → DeckDetailService (deck, stats, cards, tags, card CRUD)
│   │       └── decks.service.ts       → DecksService (CRUD + pagination + categories)
│   ├── profile/        → ProfileComponent (stub)
│   ├── shell/          → AppShellComponent + all layout sub-components
│   │   ├── mobile-header/         → MobileHeaderComponent (hamburger, shown on small screens only)
│   │   └── sidebar/               → SidebarComponent (collapsible, desktop + mobile overlay)
│   │       ├── sidebar-header/    → SidebarHeaderComponent (logo + collapse toggle)
│   │       ├── sidebar-nav/       → SidebarNavComponent (nav links with icons)
│   │       └── sidebar-footer/    → SidebarFooterComponent (avatar, username, email, profile panel trigger)
│   │           └── profile-panel/ → ProfilePanelComponent (view profile, theme, language, logout)
│   └── study/          → StudyComponent (stub)
└── shared/             → Reusable components with no feature-specific logic
    └── components/
        ├── card-state-badge/  → CardStateBadgeComponent (FSRS state pill; input: state: number | null)
        ├── card-type-badge/   → CardTypeBadgeComponent (card type pill; input: type: CardType)
        ├── confirm-dialog/    → ConfirmDialogComponent
        ├── language-switcher/ → LanguageSwitcherComponent
        ├── pagination/        → PaginationComponent (page strip with gap logic; input: currentPage, totalPages)
        ├── tag-pill/          → TagPillComponent (colored pill; inputs: name, hexColor, removable; output: remove)
        └── toast/             → ToastComponent
```

### Routing Pattern

All feature routes are **lazy-loaded**. Components use `export default class` so `loadComponent` needs no `.then()` callback.

Authenticated routes are **nested under `AppShellComponent`**, which acts as the layout parent. This ensures the sidebar is always visible when logged in:

```typescript
// app.routes.ts
{ path: 'auth', loadChildren: () => import('./features/auth/auth.routes'), canActivate: [noAuthGuard] }
{
  path: '',
  loadComponent: () => import('./features/shell/app-shell.component'),
  canActivate: [authGuard],
  children: [
    { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component') },
    { path: 'decks',     loadComponent: () => import('./features/decks/decks.component') },
    { path: 'study',     loadComponent: () => import('./features/study/study.component') },
    { path: 'profile',   loadComponent: () => import('./features/profile/profile.component') },
  ],
}

// login.component.ts
export default class LoginComponent { ... }
```

### State Management

Global state uses Angular **Signals** — no NgRx, no BehaviorSubjects:

- Private writable signal: `private readonly _x = signal<T>(null)`
- Public readonly surface: `readonly x = this._x.asReadonly()`
- Derived state: `readonly isX = computed(() => this._x() !== null)`

Services are injected via `inject()` (functional injection, no constructor parameters).

### Design Tokens

Three-layer token system in `styles.css`:

1. **Raw palette** — all Catppuccin Mocha/Latte color variables
2. **Semantic tokens** — theme-aware CSS custom properties (`--color-background`, `--color-primary`, etc.) swapped by `data-theme` attribute on `<html>`
3. **`@theme inline`** — Tailwind v4 block exposing semantic tokens as utility classes (`bg-background`, `text-primary`, etc.)

### i18n

Transloco v8 — translation files live in `public/i18n/{lang}.json`. Components use `TranslocoPipe` (`| transloco`), never the deprecated `TranslocoDirective` with `inlineRead`.

---

## Security Decisions

### 404 instead of 403 on unauthorized resource access (IDOR prevention)

When a user requests a resource that exists but belongs to another user, the API returns `404 Not Found` instead of `403 Forbidden`.

Returning `403` would confirm to an attacker that the resource exists, enabling enumeration: by iterating IDs and observing 403 vs 404 responses, an attacker could map out which IDs are valid. This is an IDOR (Insecure Direct Object Reference) vulnerability listed in the OWASP Top 10.

Returning `404` in both cases (resource not found, resource belongs to another user) makes the response indistinguishable. The attacker learns nothing about the existence of resources they do not own.

Applied in: `DeckService.findOwnedDeck()`, `CardService.findOwnedDeck()`.

### No 404 on unknown tag IDs in `GET /api/v1/tags/{tagId}/cards`

The same enumeration principle applies here. Returning `404` for a non-existent tag ID would allow an attacker to probe which tag IDs exist in the system. The endpoint always returns `200` with an empty array regardless of whether the tag exists or not.

### `authorName` uses the nickname (`username` field), not `firstName + lastName`

---

## CI Pipeline

A GitHub Actions workflow (`.github/workflows/ci.yml`) validates every push and pull request targeting `develop` or `main`. Two jobs run in parallel:

| Job | Steps |
|-----|-------|
| **Backend — Build & Test** | Checkout → JDK 21 (Temurin, Maven cache) → PostgreSQL 16 service container → apply `init.sql` → `mvn --batch-mode test` |
| **Frontend — Lint, Test & Build** | Checkout → Node 22 (npm cache) → `npm ci` → `ng lint` → `ng test --watch=false` → `ng build` |

**Why a real PostgreSQL, not H2?** The backend uses `hibernate.ddl-auto: none`, native JSONB columns, and `pgcrypto` — none of which H2 supports. The service container matches the exact production database engine and avoids a false sense of security from a divergent in-memory DB.

**Branch protection:** `develop` is configured to require both jobs to be green before any PR can be merged.

Deck author attribution uses the unique `username` field (the user's chosen nickname) rather than `firstName + lastName`. Full names are not unique — multiple users can share the same name. The `username` column has a unique constraint and unambiguously identifies the author.

**Implementation note:** `User` implements Spring Security's `UserDetails`, which forces an override of `getUsername()` to return the email (the authentication principal). Lombok cannot generate a getter for the `username` field because that method name is taken. A dedicated `getNickname()` method exposes the actual nickname value. Any code that needs the display username must call `getNickname()`, not `getUsername()`.
