# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

---

## [0.30.0] — 2026-05-27 — feat/53-study-session-persistence

### Added
- **Session backup** — after each card rating, `SessionBackupService` writes a `SessionBackup` snapshot to `localStorage` (`reitera_session_backup`). Backups expire after 24 hours and carry a `version: '1'` guard for forward-compatibility.
- **Recovery flow** — on load, `StudySessionComponent` checks for an existing backup that matches the current scope (`deckId` / `categoryId`). If found, a `'recovery'` state is shown instead of fetching due cards; the user can submit the recovered session immediately or discard it and start fresh.
- **Deactivation guard** — `studySessionGuard` (`CanDeactivateFn`) in `core/guards/study-session.guard.ts` blocks navigation away from `/study` while ratings are pending; shows a `warning` `ConfirmDialogComponent` before allowing the user to leave.
- **`beforeunload` beacon** — `StudySessionComponent` registers a `beforeunload` listener that fires `navigator.sendBeacon` to `POST /api/v1/study/sessions` when the tab is closed mid-session with ratings in memory, as a best-effort last-resort save.
- `StudyStateService` — new singleton service tracking `hasPendingRatings` (signal) and coordinating the async confirm-or-cancel deactivation flow via an RxJS `Subject`.
- `SessionBackup` interface added to `core/models/study.model.ts`.
- `study.session.deactivateDialog.*` and `study.session.recovery.*` i18n keys added to all four language files (EN, ES, FR, PT).

### Changed
- `StudyService.processSession` — cards not found or failing scope verification are now **silently skipped** instead of throwing `ResourceNotFoundException` / `IllegalArgumentException`; supports recovery submissions that may reference deleted cards. `cardsReviewed` in the response now reflects only actually processed cards (may be less than submitted ratings).
- `StudySessionComponent` — `SessionState` type extended with `'recovery'`; inputs extended with `deckName` and `categoryName` (passed via query params from `StudyHubComponent`).
- `StudyHubComponent` — study links now include `deckName` / `categoryName` as query params so the backup can store a human-readable name for the recovery prompt.
- `StudyServiceTest` — two new tests: `processSession_withMissingCard_skipsItAndReturnsZero` and `processSession_withCardFromWrongDeck_skipsItAndReturnsZero`.
- `frontend/package.json` version bumped to `0.30.0`
- `backend/reitera-backend/pom.xml` version bumped to `0.30.0`

---

## [0.29.0] — 2026-05-26 — feat/120-two-button-rating-system

### Changed
- Study session rating system simplified from 4 buttons (Again / Hard / Good / Easy) to 2 buttons — **Forgotten** (rating 1) and **Remembered** (rating 3); reduces cognitive friction and produces more consistent FSRS input data
- **Option-A requeue strategy**: forgotten cards are always re-inserted at the end of the session queue with no maximum re-queue limit; the session ends naturally only when every card has been remembered
- Progress bar now counts cards whose last rating is 3 (Remembered), reaching 100% exactly when the session ends — previously counted cards seen at least once
- `CardRatingDTO` — `@Max` constraint reduced from 4 to 3; rating 2 also rejected at the service level with an explicit `IllegalArgumentException` in `StudyService.processSession`
- `CardRating` TypeScript interface — `rating` type narrowed from `1 | 2 | 3 | 4` to `1 | 3`
- `RatingButtonsComponent` / `StudyCardComponent` — output type narrowed to `1 | 3`; `againCount` signal removed; `ratings` Map retyped as `Map<number, 1 | 3>` (eliminates `as` cast in `submitSession`)
- i18n keys `again`, `hard`, `good`, `easy` replaced by `forgotten` and `remembered` in all four language files (EN, ES, FR, PT)
- `FsrsServiceTest` — renamed `schedule_newCard_againRating_schedulesEarlierThaEasy` → `schedule_newCard_forgottenRating_schedulesEarlierThanRemembered`; rating 4 replaced with rating 3
- `frontend/package.json` version bumped to `0.29.0`
- `backend/reitera-backend/pom.xml` version bumped to `0.29.0`

### What does NOT change
- `FsrsService` algorithm — receives an `int`; `W[1]`, `W[3]`, `hardPenalty` (W[15]), `easyBonus` (W[16]) become inert parameters but cause no errors; all FSRS math remains intact
- Card state transitions (Learning → Review, Relearning → Review) — still driven by the algorithm output
- `study_progress` and `review_logs` table schemas — historical ratings 2 / 4 in `review_logs` remain valid; no migration needed
- Dashboard stats — count FSRS states, not ratings

---

## [0.28.0] — 2026-05-25 — chore/tag-roles-and-public-decks-update

### Removed
- Tag system — `Tag` entity, `tags` and `card_tags` tables, `TagService`, `TagController`, `TagRepository`, `NewTagDTO`, `TagResponseDTO` deleted; removes `GET /api/v1/tags` and `GET /api/v1/tags/{tagId}/cards` endpoints
- `tagIds` and `newTags` fields removed from `CardRequestDTO`; `tags` field removed from `CardResponseDTO`; inline tag find-or-create and orphan cleanup logic removed from `CardService`
- `tagId` filter param removed from `GET /api/v1/cards`; `CardSpecification.hasTag()` removed
- Public deck visibility — `is_public` column removed from `decks` table; `isPublic` field removed from `DeckRequestDTO` and `DeckResponseDTO`; `VisibilityBadgeComponent` deleted
- `user_deck_subscriptions` table and associated `UserDeckSubscription`, `UserDeckSubscriptionId`, `UserDeckSubscriptionRepository` deleted (planned public deck download feature abandoned)
- Frontend `tag.model.ts`, `TagPillComponent`, tag combobox from `CardFormComponent`, tag filter from `CardListComponent`, `getTags()` from `CardsService` and `DeckDetailService`

### Changed
- `seed.sql` — `is_public` column removed from all deck inserts; demo tag and card_tag inserts removed
- `truncate.sql` — `tags`, `card_tags`, `user_deck_subscriptions` entries removed
- `TagServiceTest` and `TagControllerTest` deleted; `CardServiceTest`, `StudyServiceTest` updated to remove tag-related assertions — total test count reduced from ~137 to ~126
- `frontend/package.json` version bumped to `0.28.0`
- `backend/reitera-backend/pom.xml` version bumped to `0.28.0`

---

## [0.27.0] — 2026-05-25 — feat/51-forgot-reset-password-flow

### Added
- Forgot password / reset password flow (issue #113)
- `core/email/PasswordResetService` — generates UUID token, SHA-256 hashes it before storage, saves hash + 1h expiry; `sendResetToken` uses `ifPresent` to silently ignore unknown emails (anti-enumeration); `resetPassword` nullifies token fields after use (single-use guarantee)
- `TokenExpiredException` — new exception mapped to `410 Gone` by `GlobalExceptionHandler`; distinct from 400 so the frontend can show a targeted "link expired" message
- `POST /api/v1/auth/forgot-password` — always returns 200 regardless of whether the email exists; rate-limited (5 req/min per IP)
- `POST /api/v1/auth/reset-password` — validates token (400 invalid/used, 410 expired), encodes new password, nullifies token; rate-limited
- `ForgotPasswordDTO` and `ResetPasswordDTO` records with Bean Validation constraints; `ResetPasswordDTO.newPassword` enforces the same strength regex as registration
- `UserRepository.findByPasswordResetToken(String)` — Spring Data query for hash-based token lookup
- `password_reset_token VARCHAR(64) UNIQUE` and `password_reset_token_expires_at TIMESTAMP` columns added to `users` table in `init.sql`
- HTML reset email: Catppuccin Latte palette, "Reset password" CTA button, "This link expires in 1 hour" copy, fallback text link; sent via Resend SDK (`@Async`)
- Frontend `PasswordResetService` — `forgotPassword(data)` and `resetPassword(data)` wrapping the two new endpoints
- `ForgotPasswordComponent` at `/auth/forgot-password` — email form; after submit shows a deliberately vague static message regardless of whether the email was found
- `ResetPasswordComponent` at `/auth/reset-password?token=X` — reads token from query params on `ngOnInit`; handles 4 states via `@switch (tokenState())`: `valid` (form), `missing` (no token param), `expired` (410), `invalid` (400)
- "Forgot your password?" link added to `LoginComponent` between the password field and submit button
- `ForgotPasswordRequest` and `ResetPasswordRequest` interfaces added to `auth.model.ts`
- `auth.login.forgot_password`, `auth.forgot_password.*` and `auth.reset_password.*` i18n keys added to `en.json`, `es.json`, `fr.json`, `pt.json`
- `PasswordResetServiceTest` — 5 unit tests covering: silent ignore on unknown email, token save + email send on known email, 400 on invalid token, 410 on expired token, happy path (encode + nullify)
- `AuthControllerTest` — 5 new slice tests for both endpoints and all error cases

### Changed
- `RateLimitFilter` — `/forgot-password` and `/reset-password` added to `RATE_LIMITED_PATHS`
- `frontend/package.json` version bumped to `0.27.0`
- `backend/reitera-backend/pom.xml` version bumped to `0.27.0`

---

## [0.26.0] — 2026-05-22 — feat/50-mail-verification

### Added
- Email verification flow: users must verify their email before logging in
- `core/email/EmailVerificationService` — generates UUID token, SHA-256 hashes it before storage, saves hash + 24h expiry; `verifyToken` is `@Transactional` to prevent race conditions on concurrent requests
- `GET /api/v1/auth/verify?token=` — verifies the token and activates the account; 400 for invalid or expired token
- `POST /api/v1/auth/resend-verification` — resends the verification email; always 200 (anti-enumeration); rate-limited per `ip:path`
- `DataConflictException` — new exception returning 409 Conflict for duplicate email/username on registration; replaces the former generic 400
- HTML email template for verification: Catppuccin Latte palette (`#8839ef` mauve), centered CTA button, fallback text link, sent via Resend SDK
- `app.base-url` config property drives the verification link; `http://localhost:4200/auth` in dev, `https://reitera.vercel.app/auth` in prod
- Frontend `EmailVerificationService` — `verifyEmail(token)` and `resendEmail(email)` wrapping the two new endpoints
- `CheckEmailComponent` at `/auth/check-email` — post-registration screen with destination email, spam hint, resend button, and fallback state for direct navigation
- `VerifyComponent` at `/auth/verify` — auto-triggers verification on load; spinner while in-flight; error state with email input and resend form for invalid/expired tokens
- `auth.check_email.*` and `auth.verify.*` i18n keys added to `en.json`, `es.json`, `fr.json`, `pt.json`
- `ResendVerificationRequest` interface added to `auth.model.ts`

### Fixed
- `AuthService.register()` was auto-logging in via `switchMap` after registration — now fails because unverified users cannot log in; simplified to return `Observable<void>`
- Verification link pointed to `/verify` instead of `/auth/verify` — corrected by appending `/auth` to `app.base-url`
- `RateLimitFilter` bucket key was `ip` only — now `ip:path`, preventing a `/login` attack from exhausting the `/register` rate limit

### Changed
- Registration conflict returns **409 Conflict** (was 400) — frontend detects `err.status === 409` and shows a warning toast instead of a generic error
- `seed.sql` — admin user removed; only `alumno@reitera.com` demo account remains
- `application-prod.yml` — `app.base-url` set to `https://reitera.vercel.app/auth`
- `frontend/package.json` version bumped to `0.26.0`
- `backend/reitera-backend/pom.xml` version bumped to `0.26.0`

---

## [0.25.1] — 2026-05-14 — fix/memory-leak

### Security
- Registration conflict responses now return a single generic message (`"The provided data is invalid or already in use"`) regardless of whether the email or the username was already taken — prevents user enumeration by observing distinct error messages; issue #100
- `RateLimitFilter` bucket store migrated from `ConcurrentHashMap` to a Caffeine `LoadingCache` with 10-minute expiration after last access — inactive IPs are evicted automatically, eliminating unbounded memory growth under sustained unique-IP traffic; issue #100

### Changed
- `backend/reitera-backend/pom.xml` version bumped to `0.25.1`; `caffeine` added as dependency

---

## [0.25.0] — 2026-04-28 — chore/demo-deployment

### Added
- `backend/.../resources/application-dev.yml` — new dev profile with local Docker datasource, hardcoded JWT secret (dev-only), CORS to `localhost:4200`, Swagger enabled
- `backend/.../resources/application-prod.yml` — new prod profile reading all credentials from env vars (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET_KEY`, `CORS_ALLOWED_ORIGINS`); HikariCP capped at 5 connections; Swagger disabled
- Spring Boot Actuator (`spring-boot-starter-actuator`) — exposes `GET /actuator/health` with no auth required; used as Render health check path and UptimeRobot keep-alive target
- `backend/reitera-backend/Dockerfile` — multi-stage build: stage 1 compiles with `eclipse-temurin:21-jdk-alpine`, stage 2 runs the JAR with `eclipse-temurin:21-jre-alpine` and `spring.profiles.active=prod`; required because Render has no native Java runtime
- `frontend/public/manifest.webmanifest` — defines Reitera as a basic installable PWA: `display: standalone`, `theme_color: #1e1e2e`, 192 and 512 px icons
- `frontend/public/robots.txt` — allows full search engine indexing
- `frontend/public/icons/icon-192.png` and `icon-512.png` — generated from `reitera_logo.png` via ImageMagick
- `frontend/public/favicon.ico` — multi-size (16, 32, 48 px) generated from the logo
- `LICENSE` — MIT, Copyright 2026 Helder Ruiz
- `README.md` — written from scratch: tagline, badges, project description, tech stack, 3-step local setup, links to internal docs, author
- `CONTRIBUTING.md` — branching model, Conventional Commits style, PR flow, test/lint commands, contact email

### Changed
- `backend/.../resources/application.yml` — reduced to shared defaults only: port, app name, JPA dialect, `ddl-auto: none`, JWT expiration times, Actuator config; `spring.profiles.active: dev` set as default so local dev requires no extra env vars
- `backend/.../config/SecurityConfig.java` — CORS externalized: `addAllowedOrigin("http://localhost:4200")` replaced by `@Value("${app.cors.allowed-origins}")` + split-by-comma loop; `/actuator/health` added to `permitAll()`
- `frontend/src/index.html` — full SEO rewrite: proper `<title>`, `<meta name="description">`, Open Graph (`og:title`, `og:description`, `og:type`, `og:url`, `og:image`), Twitter Card, `theme-color: #1e1e2e`, `<link rel="manifest">`
- `frontend/src/environments/environment.ts` — `apiUrl` set to `https://reitera-backend.onrender.com/api/v1`
- `CLAUDE.md` — i18n file list updated to `{en,es,fr,pt}.json`

---

## [0.24.1] — 2026-04-25 — chore/security-update

### Security
- Rate limiting on `/api/v1/auth/login` and `/api/v1/auth/register` — 5 requests per minute per IP using Bucket4j token-bucket; returns `429 Too Many Requests` with `Retry-After` header when exceeded; uses `X-Forwarded-For` for real IP on Render
- HTTP security headers added to every response: `Strict-Transport-Security` (1 year, includeSubDomains), `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Frame-Options: deny`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`
- Input size constraints on all request DTOs: `UserRegisterDTO` (email ≤ 100, password ≤ 128, firstName ≤ 50, lastName ≤ 100), `RefreshRequestDTO` (token ≤ 512), `CardRequestDTO` (question ≤ 5 000, explanation ≤ 2 000, answerJson ≤ 10 KB), `DeckRequestDTO` (title ≤ 100, description ≤ 2 000, category ≤ 50), `NewTagDTO` (name ≤ 30)
- Password complexity enforced on registration: 8+ chars, at least one digit, one lowercase, one uppercase, one special character (`@#$%^&+=!`)
- Registration form mirrors backend constraints client-side (maxlength + pattern validators) to catch errors before submission
- `.gitignore` updated to document and exclude secret override files (`application-local*.yml`, `application-secret.yml`, `.env.*`)

### Changed
- `backend/reitera-backend/pom.xml` version bumped to `0.21.1`; `bucket4j-core:8.10.1` added as dependency
- `frontend/package.json` version bumped to `0.24.1`

---

## [0.24.0] — 2026-04-23 — feature/48-stats-page
### Added
- Dashboard page — fully implemented; replaces the previous stub with stat badges, activity heatmap, and last studied decks panel
- `modules/dashboard/` backend module — `DashboardController`, `DashboardService`, `DashboardStatsDTO`, `LastStudiedDeckDTO`, `DailyStudyCountDTO`
- `GET /api/v1/dashboard/stats` — returns `{ streak, totalDueToday, studiedToday }`; streak counts consecutive days backwards from today (yesterday counts if not studied today); studiedToday counts distinct cards reviewed from `review_logs`
- `GET /api/v1/dashboard/heatmap` — returns daily card review counts for the past 365 days; sparse data mapped to a dense 365-cell grid on the frontend
- `GET /api/v1/dashboard/last-studied` — returns the last N decks reviewed, with name, category, and FSRS state breakdown
- `DashboardService` in `features/dashboard/services/` — `getStats()`, `getHeatmap()`, `getLastStudied()`
- `DashboardStats` model in `core/models/dashboard.model.ts`
- `StatCardComponent` in `features/dashboard/components/stat-card/` — value + label + icon slot
- `StudyHeatmapComponent` in `features/dashboard/components/study-heatmap/` — 365-cell GitHub-style calendar grid with 4 color intensity levels aligned to calendar weeks
- `LastStudiedDecksComponent` in `features/dashboard/components/last-studied-decks/` — last N decks with state breakdown badges and conditional Study CTA
- `dashboard.*` i18n keys added to `en.json`, `es.json`, `fr.json`, `pt.json`
- Skeleton loaders for all three sections while API calls are in-flight; error state with message if stats call fails

### Changed
- `DeckRepository` — added `findAllIdsByOwner(User)` query used by dashboard stats
- `ReviewLogRepository` — added `countDistinctCardsByUserAndDate()` and `findDistinctReviewDatesByUser()` queries for studiedToday and streak calculation
- `frontend/package.json` version bumped to `0.24.0`

---

## [0.23.0] — 2026-04-20 — feature/47-user-profile-page
### Added
- `ProfileComponent` — read-only display of the authenticated user's account information: avatar circle (first-name initial), full name, @username, email, role and member-since date
- `profile.*` i18n keys added to all 4 language files (`en.json`, `es.json`, `fr.json`, `pt.json`)
- `UserResponseDTO` extended with `createdAt` field; `UserService` maps it from the entity

### Fixed
- Card list state filter — removed duplicate "Not studied" option (value `-1`); the concept is already covered by the "New" state; removed now-unused `stateNotStudied` i18n keys

---

## [0.22.0] — 2026-04-19 — feature/46-admin-card-page
### Added
- Cross-deck card list page at `/cards` — shows all cards owned by the user across every deck; accessible from the sidebar nav ("Cards" link with `SquareStack` icon)
- Filter bar with four independent filters: partial question search, card type (`BASIC` / `MULTIPLE_CHOICE` / `TRUE_FALSE`), FSRS study state (not studied / New / Learning / Review / Relearning), and tag; all combinable; "Clear filters" button resets all at once
- `GET /api/v1/cards` backend endpoint — paginated, cross-deck card query with optional `question`, `type`, `state`, and `tagId` filter params; filters composed dynamically via JPA Specifications; FSRS states enriched in a single batch query (N+1 free)
- `CardSpecification` — static JPA Specification factory methods: `byOwner`, `questionContains`, `byType`, `hasTag`, `notStudied`, `withState`
- `CardListController` at `/api/v1/cards` — thin controller delegating to `CardService.getAllCards()`
- `CardsService` in `features/card-list/services/` — `getCards()` (paginated + filtered), `getTags()`, `updateCard()`, `deleteCard()`
- `FilterDropdownComponent` in `shared/components/filter-dropdown/` — reusable click-outside-aware dropdown for filter options; supports `labelKey` (i18n) or `label` (raw string) and optional `colorHex` for tag pills
- Global scrollbar styling in `styles.css` — thin, theme-aware scrollbar using `--color-border` / `--color-border-strong` tokens

### Changed
- `CardRepository` — extended with `JpaSpecificationExecutor<Card>` to support dynamic filter composition
- `StudyProgressRepository` — added `findAllByUserIdAndCardIdIn` batch query for N+1-free state enrichment
- `CardService` — added `getAllCards()` method with specification-based filtering
- `CardsTableComponent` — added `emptyTitle`, `emptySubtitle`, and `showAddAction` inputs so the component is reusable outside the deck-detail context (card-list page omits the "Add card" action)
- `SidebarNavComponent` — added "Cards" nav link with `LucideSquareStack` icon
- `app.routes.ts` — added `/cards` lazy route pointing to `CardListComponent`
- `docs/api/cards.md` — documented new `GET /api/v1/cards` endpoint and corrected `state` field description
- `frontend/package.json` version bumped to `0.22.0`

---

## [0.21.0] — 2026-04-18 — feature/43-study-page
### Added
- Study hub (`StudyComponent`) — deck picker with per-deck new/due/relearning counts; category-scoped study button; empty state when no decks exist
- Study session (`StudySessionComponent`) — full card review flow: progress bar, reveal/rate cycle, Again re-queue (up to 3 times), end-session button, completion screen
- `StudyCardComponent` — renders all 3 card types (BASIC, MULTIPLE_CHOICE, TRUE_FALSE); shows explanation after reveal
- `StudyHubComponent` — deck picker page with `<app-card-count-badges>` and category study shortcut
- `StudyService` in `features/study/services/` — `getDueCards(deckId?, categoryId?)` and `processSession(request)` wrappers over the study API
- Back-button guard — clicking the back arrow during an active session shows a `warning` `ConfirmDialogComponent`; the session is abandoned only on confirmation, never silently
- Shared components: `EmptyStateComponent` (icon + title + subtitle + action slot), `PageHeaderComponent` (title + subtitle), `CardCountBadgesComponent` (new/due/relearning pill group)
- Portuguese (`pt`) language support — `public/i18n/pt.json` created (European Portuguese); `availableLangs` in `app.config.ts` updated to `['en', 'es', 'fr', 'pt']`
- `DeckResponse` model updated with `newCount`, `dueCount`, `relearningCount` fields consumed by the study hub badges

### Changed
- `StudyComponent` — replaced the previous stub with the full study hub and session routing
- `seed.sql` — expanded from 21 to 48 cards (12 per deck), all 3 card types represented in every deck; 4 new `study_progress` rows added across Decks 2 and 3 for a more realistic demo

---

## [0.20.0] — 2026-04-14 — feature/40-ci-pipeline
### Added
- `.github/workflows/ci.yml` — GitHub Actions CI pipeline with two independent parallel jobs:
  - **Backend — Build & Test**: sets up JDK 21 (Temurin) with Maven cache; starts a PostgreSQL 16 service container (same credentials as local dev); applies `init.sql` via `psql`; runs `mvn --batch-mode test`
  - **Frontend — Lint, Test & Build**: sets up Node 22 with npm cache; runs `npm ci`, `npx ng lint`, `ng test --watch=false` (Vitest, no browser), and `ng build` (production)
- Pipeline triggers on `push` and `pull_request` targeting `develop` and `main`
- Maven (`~/.m2`) and npm dependencies cached between runs to reduce job duration

---

## [0.19.0] — 2026-04-12 — feature/38-deck-detail-page
### Added
- `DeckDetailComponent` — full deck detail page at `/decks/:id`; shows breadcrumb, deck header (title, description, category, visibility, created date), FSRS stats chips, and a paginated sortable cards table
- `DeckDetailService` — feature-scoped service handling `GET /decks/{id}`, `GET /decks/{id}/stats`, `GET /decks/{id}/cards`, `GET /tags`, card CRUD
- `GET /api/v1/decks/{id}/stats` backend endpoint — returns `DeckStatsDTO` with total, new, learning, review, relearning and due card counts; all counts derived from `StudyProgress` in a single service call
- Card table sorting by question or type — sort applied on the backend via `Pageable` (`?sort=field,direction`); state column excluded from sorting (computed in-memory from `StudyProgress`)
- `CardFormComponent` — create/edit dialog modal for cards; stays open after create so users can add multiple cards without reopening; closes after edit
- Tag system (full implementation):
  - Tags are user-scoped: `UNIQUE(name, owner_id)` DB constraint; two users can share the same tag name independently
  - Find-or-create: user types a tag name in the card form → existing tag is reused, new tag is created on card save
  - Tags carry a hex color chosen from a 10-color predefined palette in the form UI
  - `newTags` field added to `CardRequestDTO` — carries `name + hexColor` pairs for inline tag creation
  - Orphan cleanup: tags with no remaining cards are deleted automatically after `updateCard` / `deleteCard` (same pattern as category cleanup in `DeckService`)
  - `TagRepository` — added `findAllByOwnerId`, `findByNameIgnoreCaseAndOwnerId`, `countCardsByTagId`
  - `TagService.findOrCreateTag()` — core find-or-create method, `@Transactional`
  - `CardService` — `createCard`, `updateCard`, `deleteCard` are now `@Transactional`; added `resolveTags()` and `cleanupOrphanTags()` private helpers

- Component extraction refactor:
  - `<app-pagination>` at `shared/components/pagination` — `pageRange` computed moved inside; used in `DecksComponent` and `DeckDetailComponent`
  - `<app-card-type-badge>` at `shared/components/card-type-badge` — encapsulates type pill color logic; input: `type: CardType`
  - `<app-card-state-badge>` at `shared/components/card-state-badge` — encapsulates FSRS state pill color logic; input: `state: number | null`
  - `<app-cards-table>` at `features/decks/components/cards-table` — full cards table with loading skeleton, empty state, sorting headers; outputs: `sort`, `edit`, `delete`, `addCard`; reusable for the future all-cards page
  - `<app-tag-pill>` at `shared/components/tag-pill` — colored pill with optional remove button; inputs: `name`, `hexColor`, `removable`; output: `remove`
  - `<app-visibility-badge>` at `features/decks/components/visibility-badge` — public/private pill; used in `DeckCardComponent` and `DeckDetailComponent`

- Study button in `DeckDetailComponent` routes to `/study?deckId=X` (query param, not nested route, so the study page can work without a deck filter)

### Changed
- `Tag.java` — added `owner: User` (`@ManyToOne LAZY`); `name` column uniqueness moved from `@Column(unique=true)` to a DB-level `UNIQUE(name, owner_id)` constraint
- `init.sql` — added `DROP TABLE IF EXISTS … CASCADE` block at the top so the script is safe to re-run in development; `tags` table updated with `owner_id` column and `UNIQUE(name, owner_id)` constraint
- `seed.sql` — tag inserts updated to include `owner_id`; `ON CONFLICT` updated to `(name, owner_id)`
- `DeckCardComponent` — now uses `<app-visibility-badge>` instead of inline badge markup
- `DeckDetailComponent` — cards table replaced with `<app-cards-table>`; visibility badge replaced with `<app-visibility-badge>`
- `DecksComponent` and `DeckDetailComponent` — `pageRange` computed and Chevron icon imports removed; pagination replaced with `<app-pagination>`
- `CardFormComponent` — tag combobox replaces static toggle pills; dynamic `FormArray` replaces hardcoded 4-option MC controls (min 2, max 8); form stays open after create

### Fixed
- SonarQube S7723 — `Array(n)` → `new Array(n)` in `card-form.component.ts`
- SonarQube S7778 — consecutive `FormArray.push()` calls → `form.setControl('options', fb.array([...]))` in `card-form.component.ts`
- SonarQube S6847 — added keyboard handlers to clickable divs in `deck-card.component.html` and `deck-form.component.html`
- SonarQube S6853 — toggle switch `<label>` in `deck-form.component.html` now has accessible text via `<span class="sr-only">`
- Delete confirm dialog was closing the card form on cancel — `(deleted)` output now only calls `requestDeleteCard()`; form is closed only after the API confirms deletion in `confirmDeleteCard()`

---

## [0.18.0] — 2026-04-04 — fix/36-deck-pagination-and-category-filter
### Fixed
- `GET /api/v1/decks` — added optional `categoryId` query param; `DeckRepository` gained `findAllByOwnerAndCategory(User, Category, Pageable)` and `DeckService.getUserDecks()` now delegates to it when `categoryId` is present, so pagination and filtering are applied together on the backend
- `DecksComponent.onDeckSaved()` — create path now calls `loadDecks()` instead of locally prepending the deck, so `totalPages` is recalculated from the backend response and the pagination bar appears/disappears correctly
- `DecksComponent.confirmDeleteDeck()` — now calls `loadDecks()` instead of locally filtering the array, keeping `totalPages` in sync after deletion
- `DecksComponent.setActiveCategory()` — resets `currentPage` to `0` before calling `loadDecks()`, preventing out-of-range page requests when switching categories
- `DecksService.getDecks()` — accepts optional `categoryId` and appends it as a query param when provided

### Changed
- `filteredDecks` computed signal removed from `DecksComponent` — filtering is now fully server-side; template uses `decks()` directly
- `docs/api/endpoints.md` — `GET /api/v1/decks` table updated with the new `categoryId` param

---

## [0.17.0] — 2026-04-04 — fix/34-jwt-refresh
### Fixed
- `JwtAuthenticationFilter` — added `try-catch (JwtException)` around `extractUsername()`; on failure the filter now calls `filterChain.doFilter` and returns early, leaving the `SecurityContext` empty so Spring Security can respond with a clean `401`. Previously the uncaught `ExpiredJwtException` propagated to Spring Boot's error dispatcher, which hit the unprotected `/error` path and returned a `403` with no body
- `SecurityConfig` — added `.exceptionHandling()` with a custom `AuthenticationEntryPoint` that returns `401 Unauthorized`; the default `Http403ForbiddenEntryPoint` was causing all unauthenticated requests to return `403` instead of the correct `401`
- `jwtInterceptor` — rewritten to handle the full token refresh cycle: on `401`, reads the stored `refreshToken`, calls `POST /auth/refresh`, persists the new token pair, and retries the original request transparently; if the refresh call fails the interceptor calls `AuthService.logout()` and redirects to login; `/auth/` requests are excluded from the retry logic to prevent infinite loops

### Changed
- `ARCHITECTURE.md` — access token storage corrected from "Client memory" to `localStorage`; security flow updated to document the `JwtException` handling, `AuthenticationEntryPoint`, and the `jwtInterceptor` refresh cycle

---

## [0.16.0] — 2026-04-03 — feature/31-deck-page
### Added
- `DecksService` in `features/decks/services/` — full CRUD: `getDecks(page, size)` with backend pagination (`?sort=createdAt,desc`), `getCategories()`, `createDeck()`, `updateDeck()`, `deleteDeck()`
- `DeckCardComponent` — displays a single deck with accent color (cycled from six `--color-accent-*` tokens by `id % 6`), visibility badge, last-updated date, and a click-outside-aware options menu with edit and delete outputs
- `CategoryFilterComponent` — collapsible dropdown filter; emits `categorySelected` output; closes on outside click via `@HostListener` + `data-category-filter` attribute guard; "All" option clears the filter
- `DeckFormComponent` — modal dialog for create and edit; Angular Reactive Form with title (required, max 100), description, category (`<datalist>` autocomplete against existing categories), and isPublic toggle; edit mode pre-populated via `effect()`; on save in edit mode a warning `ConfirmDialogComponent` is shown before the API call; category resolved as find-or-create: existing match by name → `categoryId`, no match → `categoryName` forwarded to backend
- `ConfirmDialogComponent` in `shared/components/confirm-dialog/` — generic confirm dialog with `variant` input (`danger` | `warning` | `success` | `info`); icon, button color and background driven by `VARIANT_CONFIG` map; configurable `title`, `message` (supports Transloco interpolation) and `confirmLabel`; `confirmed` and `cancelled` outputs; backdrop click cancels
- Pagination UI in `DecksComponent` — prev/next buttons + page-number strip with gap logic (`pageRange` computed signal: always shows first, last, current ±1, with `…` gaps); disabled states on edges
- `filteredDecks` computed signal — client-side category filter applied on top of the paginated page
- Toast notifications (via `ToastService`) on deck created, updated and deleted
- i18n keys added to `en.json`, `es.json` and `fr.json`: `decks.filter`, `decks.pagination`, `decks.card.*`, `decks.empty.*`, `decks.form.*`, `decks.edit.*`, `decks.delete.*`, `decks.toast.*`, `common.cancel`, `common.confirm`, `common.delete`

### Changed
- `DecksComponent` — fully implemented; replaced the previous stub with paginated grid, category filter, CRUD dialogs and delete/edit-save confirm flows
- `ConfirmDialogComponent` added to `shared/components/` (previously only `ToastComponent` and `LanguageSwitcherComponent` lived there)

---

## [0.15.0] — 2026-03-30 — feature/30-category-lifecycle
### Added
- `CategoryRepository.findByNameIgnoreCase(String name)` — case-insensitive lookup used by find-or-create
- `DeckRepository.countByCategory(Category category)` — used by orphan cleanup to check whether a category is still referenced

### Changed
- `DeckRequestDTO` — added optional `categoryName` field (max 50 chars); `categoryId` continues to work and takes precedence when both are supplied
- `DeckService.createDeck()` — replaced `resolveCategory` with `findOrCreateCategory`: uses `categoryId` if provided, otherwise finds or creates by `categoryName`; annotated `@Transactional`
- `DeckService.updateDeck()` — same find-or-create logic; saves the old category reference before reassignment and runs orphan check after save; annotated `@Transactional`
- `DeckService.deleteDeck()` — saves the category reference before deletion and runs orphan check after; annotated `@Transactional`

---

## [0.14.0] — 2026-03-29 — feature/28-app-shell-sidebar
### Added
- `AppShellComponent` in `features/shell/` — layout wrapper for all authenticated routes; `<router-outlet>` renders child views inside the main content area
- `ThemeService` in `core/theme/` — reads `prefers-color-scheme` on first load; persists user preference in `localStorage`; applies `data-theme` attribute on `<html>` reactively via Angular `effect()`
- `SidebarComponent` — collapsible aside with three zones (header, nav, footer); expanded/collapsed state persisted in `localStorage` (`sidebar_collapsed`); smooth CSS `transition-all` on width change
- `SidebarHeaderComponent` — Reitera logo button that triggers collapse/expand; title hidden when collapsed
- `SidebarNavComponent` — three nav links (Dashboard, Decks, Study) with Lucide icons, active-route highlight, and `navItemClicked` output to close the mobile overlay on navigation
- `SidebarFooterComponent` — displays avatar initial, username and email; opens the profile panel on click; closes panel on outside click via `@HostListener` + `data-profile-menu` attribute guard
- `ProfilePanelComponent` — fixed-position dropdown with user info, **View profile** link → `/profile`, theme toggle, language switcher and logout; navigation closes both the panel and the mobile sidebar overlay
- `MobileHeaderComponent` — top bar shown only on small screens; Reitera logo and hamburger button that triggers the mobile sidebar
- Mobile sidebar overlay — ¾-width slide-in panel with `translate-x` CSS transition; backdrop closes it on click; nav and profile navigation also close it automatically
- `ProfilePanelService` stub registered in `core/profile-panel/` — scaffolded for future use
- Stub routes `/decks`, `/study`, `/profile` registered and connected to the shell
- `shell.*` i18n keys added to `en.json`, `es.json` and `fr.json` (nav labels, sidebar actions, profile menu options)

### Changed
- `AuthService` rewritten — replaces client-side JWT decode with `GET /users/me` after login/register and on page reload, so the full user profile (username, email) is available in the `currentUser` signal
- `app.routes.ts` — all authenticated routes nested under the shell route with `authGuard`; `AppShellComponent` acts as the lazy-loaded layout parent
- `frontend/package.json` version bumped to `0.14.0`

---

## [0.13.0] — 2026-03-28 — feature/12-auth-login-register
### Added
- `AuthService` in `core/auth/` — wraps `POST /auth/login`, `POST /auth/register` and client-side logout; stores `accessToken` and `refreshToken` in `localStorage`; exposes a readonly `currentUser` signal and `isLoggedIn` computed; restores user from stored token on page reload
- `LoginComponent` — reactive form with email + password fields, inline field validation, and link to register
- `RegisterComponent` — reactive form with all fields (`username`, `email`, `firstName`, `lastName`, `password`, `confirmPassword`); cross-field password match validator; inline field validation; link to login
- `authGuard` — functional `CanActivateFn` that redirects unauthenticated users to `/auth/login`
- `noAuthGuard` — functional `CanActivateFn` that redirects already-authenticated users away from auth routes to `/dashboard`
- `auth.routes.ts` in `features/auth/` — lazy routes for `/auth/login` and `/auth/register` using `loadComponent` with `export default`
- `DashboardComponent` — stub component registered as the post-login landing route at `/dashboard`
- `app.routes.ts` updated — root redirect to `/auth/login`, auth feature via `loadChildren`, dashboard via `loadComponent`; both protected by their respective guards
- `ToastService` in `core/toast/` — singleton signal-based notification service with `success`, `error`, `warning` and `info` helpers; auto-dismiss via `setTimeout`; manual `dismiss(id)`
- `ToastComponent` in `shared/components/toast/` — fixed bottom-right overlay; `@for` loop over `toastService.toasts()`; animated progress bar that shrinks over the toast duration; `LucideX` dismiss button
- `LanguageSwitcherComponent` in `shared/components/language-switcher/` — click-outside-aware dropdown that calls `TranslocoService.setActiveLang()`; integrated into both auth screens
- `Toast` model and `ToastType` union type added to `core/models/toast.model.ts`
- i18n keys for auth (login, register) and toast copy added to `en.json`, `es.json` and `fr.json`
- Server errors on login and register shown via the global toast system

---

## [0.12.0] — 2026-03-27 — feature/13-frontend-base-config
### Added
- TypeScript path aliases in `tsconfig.json`: `@core/*`, `@features/*`, `@shared/*`, `@environments/*` — all imports use aliases instead of relative paths
- Catppuccin design token system in `styles.css`:
  - Raw palette layer: all Catppuccin Mocha (dark) and Latte (light) named color variables
  - Semantic token layer: theme-aware CSS custom properties (foreground, background, surface, primary, success, error, warning, info, border) defined in `:root` (dark) and `[data-theme='light']`
  - `@theme inline` block exposing all semantic tokens as Tailwind utilities (`bg-background`, `text-primary`, etc.)
- Theme switching via `data-theme` attribute on `<html>` — dark is default; light applied by setting `data-theme="light"`
- `@jsverse/transloco` v8 configured — `TranslocoHttpLoader`, `availableLangs: ['en', 'es', 'fr']`, `defaultLang: 'en'`, `reRenderOnLangChange: true`
- Translation files `public/i18n/en.json`, `es.json`, `fr.json` with `common.*` namespace
- `angular.json` schematics config: `"type": "component"` and `"type": "service"` for `.component.ts` / `.service.ts` file suffixes

---

## [0.11.0] — 2026-03-27 — feature/11-frontend-setup
### Added
- Angular 21 project scaffolded in `frontend/` — standalone components, no NgModules, Tailwind CSS v4 via PostCSS, Vitest for testing
- Environment files (`environment.ts`, `environment.development.ts`) — `apiUrl` configured for local backend at `http://localhost:8080/api/v1`
- Feature-based folder structure: `core/` (auth, guards, interceptors, models), `shared/components/`, `features/` (auth, dashboard, decks, cards, study, profile)
- TypeScript models in `core/models/` mapping all backend DTOs: `auth`, `user`, `deck`, `card`, `study`, `category`, `tag`, `page`
- `Page<T>` generic model for paginated responses
- `HttpClient` registered in `app.config.ts` via `provideHttpClient(withInterceptors([jwtInterceptor]))`
- `jwt.interceptor.ts` — functional interceptor that reads `accessToken` from localStorage and attaches `Authorization: Bearer` header to every outgoing request
- `auth.guard.ts` — functional guard that protects routes requiring authentication, redirecting unauthenticated users to `/auth/login`

### Changed
- `app.html` — replaced Angular boilerplate with `<router-outlet />`
- `app.ts` — removed unused `signal` boilerplate from root component

---

## [0.10.0] — 2026-03-24 — feature/10-cors-swagger-pagination
### Added
- CORS policy configured in `SecurityConfig` — allows `http://localhost:4200` (Angular dev server) with `Authorization` and `Content-Type` headers; credentials enabled; preflight cached for 1 hour
- `springdoc-openapi-starter-webmvc-ui:3.0.2` dependency — auto-generates OpenAPI schema at `/v3/api-docs` and serves Swagger UI at `/swagger-ui.html`
- `OpenApiConfig` — declares API metadata (`title`, `version`, `description`) and registers a global `bearerAuth` Bearer JWT security scheme, enabling the "Authorize" button in Swagger UI
- `@SecurityRequirement(name = "bearerAuth")` added to all protected controllers so Swagger UI marks them with the lock icon and attaches the token automatically
- Pagination support on `GET /api/v1/decks` and `GET /api/v1/decks/{deckId}/cards`:
  - Both endpoints accept `?page`, `?size`, and `?sort` query params
  - Response shape changed from `List<DTO>` to `Page<DTO>` (adds `totalElements`, `totalPages`, `number`, `size`, `first`, `last`)
  - Default: `size=20`; decks additionally default to `sort=createdAt,DESC`
- Pagination support on `GET /api/v1/tags/{tagId}/cards` — same `Page<CardResponseDTO>` shape, default `size=20`

### Changed
- `SecurityConfig` — Swagger UI routes (`/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`) added to `permitAll()` so documentation is publicly accessible
- `DeckRepository.findAllByOwner` signature updated to accept `Pageable` and return `Page<Deck>`
- `CardRepository.findAllByDeck` signature updated to accept `Pageable` and return `Page<Card>`

---

## [0.9.0] — 2026-03-22 — feature/09-refresh-token
### Added
- Refresh token system: two-token authentication strategy (access token + refresh token)
- `POST /api/v1/auth/refresh` — issues a new access token and a rotated refresh token given a valid refresh token
- `POST /api/v1/auth/logout` — revokes all active refresh tokens for the authenticated user
- `RefreshToken` entity mapped to the new `refresh_tokens` table (SHA-256 hash storage, revoked flag, expiry)
- `RefreshTokenRepository` with JOIN FETCH query to avoid N+1 on user+role loading
- `RefreshTokenService` — full lifecycle: create, validate, rotate, revoke
- `RefreshRequestDTO` — request body for `/refresh` and `/logout`
- `refresh_tokens` table added to `init.sql` with `ON DELETE CASCADE` FK and `idx_refresh_tokens_user_id` index
- `refresh_tokens` added to `truncate.sql`
- `AuthController` moved to `modules/auth/controller/` package (consistent with project structure)

### Added
- `InvalidRefreshTokenException` — dedicated exception for invalid/expired/revoked refresh tokens, mapped to HTTP 401 by `GlobalExceptionHandler`. Previously these cases fell through to the generic `RuntimeException` handler and returned HTTP 400.

### Fixed
- `POST /api/v1/auth/register` and `GET /api/v1/users/me` — `username` field in the response was returning the email instead of the user's chosen nickname. Root cause: Spring Security's `UserDetails.getUsername()` override returns the email (authentication principal); Lombok cannot generate a getter for the `username` field because the method name is taken. Fixed by adding `getNickname()` to `User` and using it in `UserService` and `DeckService`.
- `DeckService.createDeck()` — `authorName` was being set to the user's email instead of their nickname for the same reason. Fixed alongside the above.

### Changed
- `POST /api/v1/auth/login` — response now returns `accessToken` + `refreshToken` + `message` (was `token` + `message`)
- Access token expiry reduced from 24 hours to 15 minutes (`expiration-time: 900000` ms)
- `application.yml` — new `api.security.refresh-token.expiration-days: 7` property
- `RefreshTokenRepository.findByTokenHash` uses `JOIN FETCH rt.user u JOIN FETCH u.role` to resolve token + user + role in a single SQL query (N+1 prevention)

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
- Postman collection updated with Users, Categories and Tags folders; Login split into alumno / admin variants

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
  - 21 cards covering all 3 types: `BASIC`, `MULTIPLE_CHOICE`, `TRUE_FALSE`
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
  - Supported types: `BASIC`, `MULTIPLE_CHOICE`, `TRUE_FALSE`
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
