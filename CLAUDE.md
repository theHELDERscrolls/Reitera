# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Reitera spaced repetition flashcard app. Monorepo with two independent projects:
- `frontend/` — Angular 21 SPA
- `backend/reitera-backend/` — Spring Boot 4.1.0 REST API

## Commands

### Frontend (`frontend/`)
```bash
ng serve             # dev server on http://localhost:4200
ng build             # production build
ng test              # unit tests (Vitest + jsdom)
ng lint              # ESLint
```

### Backend (`backend/reitera-backend/`)
```bash
mvn spring-boot:run  # dev server on http://localhost:8080
mvn test             # run tests
mvn clean package    # build JAR
```

### Database (root)
```bash
docker-compose up -d         # start PostgreSQL 16 on port 5432
# Then in psql (reitera_db):
\i src/main/resources/init.sql      # create schema
\i src/main/resources/seed.sql      # load demo data
\i src/main/resources/truncate.sql  # reset data (preserves schema)
```
Credentials: `reitera_admin` / `reitera_password` / `reitera_db`

## Architecture

### Backend structure
```
modules/
  auth/    — JWT filter, refresh token rotation, login/register
  user/    — User entity, GET /users/me (profile) + PUT /users/me (edit personal data + avatar)
  deck/    — Deck + Category entities, CRUD with find-or-create category and orphan cleanup
  note/    — Note entity + NoteParser (Markdown → cards auto-generation), CRUD nested under decks
  card/    — Card entity (JSONB answer_json); types: BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE; read-only from client
  study/   — FSRS-6 algorithm, StudyProgress (per user+card), ReviewLog
config/    — SecurityConfig (JWT chain), ApplicationConfig, OpenApiConfig
core/      — GlobalExceptionHandler, custom exceptions
```

Schema is managed manually (`hibernate.ddl-auto: none`). Never rely on Hibernate to create or alter tables — edit `init.sql`.

**Security:** Stateless JWT (15 min access + 7 day refresh). All endpoints require auth except `/api/v1/auth/**` and Swagger. IDOR prevention: every deck/card operation calls `findOwnedDeck(id, owner)` — never trust the client's claimed ownership.

**Category lifecycle:** `DeckService` uses find-or-create (`findByNameIgnoreCase` or create new) on save, and orphan cleanup (`countByCategory == 0` → delete) on update/delete. Both operations are `@Transactional`.

### Frontend structure
```
core/
  auth/      — AuthService (signals), JWT interceptor
  guards/    — authGuard, noAuthGuard, studySessionGuard
  models/    — Shared interfaces (see Models section below)
features/
  auth/      — Login, register, check-email and verify pages
               services: EmailVerificationService
  shell/     — AppShellComponent layout (collapsible sidebar, mobile header)
  decks/     — Deck list, detail, form, cards table, note editor, category filter
               services: DecksService, DeckDetailService, NoteService
  cards/     — Cross-deck card listing with filters
  study/     — Study hub (deck picker) + study session (card review flow, Markdown rendering via ngx-markdown)
               components: study-hub, study-session, study-card, card-explanation, session-recovery-notice
               services: StudyService, SessionBackupService, StudyStateService
  dashboard/ — Streak, cards due today, activity heatmap and last-studied decks
               components: stat-card, study-heatmap, last-studied-decks
               services: DashboardService
  profile/   — Editable user profile (firstName, lastName, username, avatar picker)
               services: ProfileService
shared/
  components/ — Toast, ConfirmDialog, Pagination,
                CardStateBadge, CardTypeBadge, LanguageSwitcher
```

**Path aliases** (use these, not relative imports):
- `@core/*` → `src/app/core/*`
- `@features/*` → `src/app/features/*`
- `@shared/*` → `src/app/shared/*`
- `@environments/*` → `src/environments/*`

### Models (`core/models/`)

| File | Key exports |
|---|---|
| `auth.model.ts` | Login/register DTOs, token response, `ResendVerificationRequest` |
| `card.model.ts` | `CardResponse`, `CardType` union (`BASIC` \| `BASIC_REVERSE` \| `CLOZE` \| `MULTIPLE_CHOICE`) |
| `note.model.ts` | `NoteRequest`, `NoteResponse`, `NoteType` union (`BASIC` \| `BASIC_REVERSE` \| `CLOZE` \| `MULTIPLE_CHOICE` \| `UNKNOWN`) |
| `category.model.ts` | `Category` |
| `deck.model.ts` | `DeckRequest`, `DeckResponse` (includes `newCount`, `dueCount`, `relearningCount`), `DeckStats` |
| `page.model.ts` | Generic `Page<T>` for paginated responses |
| `study.model.ts` | `DueCard`, `CardRating`, `StudySessionRequest`, `StudySessionResponse`, `CardState` (0–3), `SessionBackup` |
| `toast.model.ts` | `Toast`, severity levels |
| `user.model.ts` | `User`, `UpdateUserRequest` |

### Key frontend patterns

**Always use `ng generate`** for new Angular elements — never create files manually.

**Standalone components** with `export default class`. All routes are lazy-loaded.

**State management:** Angular Signals only — `signal()`, `computed()`, `input.required<T>()`, `output<T>()`. No NgRx, no BehaviorSubject for component state.

**Services scope:** `core/` is for singleton services any feature could need (auth, theme, toast). Feature-specific services go in `features/<name>/services/` with `providedIn: 'root'`.

**i18n:** Transloco. Translation files at `public/i18n/{en,es,fr,pt}.json`. Always add keys to all four files. Use `TranslocoService.translate('key')` before passing strings to `ToastService` (it stores raw strings).

**Theming:** Catppuccin Mocha (dark) / Latte (light). Design tokens in `styles.css` via `@theme inline`. Accent colors `--color-accent-1` through `--color-accent-6` available for component variety.

**Toast:** Container uses `pointer-events-none`; individual toasts use `pointer-events-auto`. Do not change this — the container was blocking clicks when empty.

**Testing:** Vitest 4 + jsdom via `@angular/build:unit-test`. Key rules:
- HTTP services: `provideHttpClient()` + `provideHttpClientTesting()`; call `httpMock.verify()` in `afterEach`.
- Functional guards: `TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot))` with `provideRouter([])`.
- Signals are synchronous — no `fakeAsync`/`tick()`/`await`.
- Services that read `localStorage` in a property initializer (e.g. `readonly backup = signal(this.load())`): set `localStorage` **before** `TestBed.inject()`.
- No `any` casts — import and use real Angular types (`ActivatedRouteSnapshot`, `RouterStateSnapshot`).
- Empty callbacks in `.subscribe()` error handlers: use `error: () => undefined`, not `error: () => {}`.

### API base
`http://localhost:8080/api/v1` in development (configured in `src/environments/environment.development.ts`).

## Database schema notes

- `study_progress` is keyed `(user_id, card_id)` — FSRS state is per user per card, not per deck. Two users studying the same deck have fully independent progress rows.
- Cards are generated automatically from **notes**. A note's Markdown content is parsed by `NoteParser` into one or more cards. Supported types: `BASIC` (1 card), `BASIC_REVERSE` (2 cards — forward + reverse), `CLOZE` (one card per `{{cN::}}` deletion), `MULTIPLE_CHOICE` (1 card). Type-specific JSON is stored in `answer_json` (JSONB).
