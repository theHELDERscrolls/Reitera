# Frontend Architecture

The Angular 21 frontend is a **Single-Page Application** using standalone components — no NgModules anywhere.

## Folder Structure

```
src/app/
├── core/               → Singleton services, guards, interceptors, and models shared across all features
│   ├── auth/           → Authentication logic, token storage, and user session
│   ├── guards/         → Route activation and deactivation guards
│   ├── interceptors/   → HTTP interceptor: attaches JWT and handles transparent token refresh on 401
│   ├── models/         → TypeScript interfaces for all backend DTOs
│   ├── profile-panel/  → Profile panel helper service
│   ├── theme/          → Dark/light theme switching and persistence
│   └── toast/          → Global notification service
├── features/           → One folder per feature; each has its own routes file and components
│   ├── auth/           → Login, register, email verification, and password reset pages
│   │   └── services/   → EmailVerificationService, PasswordResetService
│   ├── dashboard/      → Overview: streak, cards due today, activity heatmap, last studied decks
│   ├── decks/          → Deck list with category filter, deck CRUD, and note management within a deck
│   │   ├── components/ → CardsTableComponent, CategoryFilterComponent, DeckCardComponent, DeckDetailComponent, DeckFormComponent, NoteEditorComponent
│   │   └── services/   → DecksService (deck CRUD + pagination), DeckDetailService (deck stats + card listing), NoteService (note CRUD)
│   ├── card-list/      → Cross-deck card list with filters (question, type, state) and inline edit/delete
│   │   └── services/   → CardsService
│   ├── profile/        → Read-only display of the authenticated user's profile
│   ├── shell/          → App layout: collapsible sidebar and mobile header
│   │   ├── mobile-header/  → Top bar with hamburger button for small screens
│   │   └── sidebar/        → Collapsible desktop sidebar and mobile overlay
│   │       ├── sidebar-header/    → Logo and collapse toggle
│   │       ├── sidebar-nav/       → Main navigation links
│   │       └── sidebar-footer/    → User avatar and profile panel trigger
│   │           └── profile-panel/ → Dropdown with theme, language, and logout actions
│   └── study/          → Study hub and active card review session
│       ├── components/
│       │   ├── study-hub/             → Deck picker with card count badges and pending session recovery banner
│       │   ├── study-session/         → Card review flow with progress tracking, backup, and recovery
│       │   ├── study-card/            → Renders a card (BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE) and handles reveal; uses ngx-markdown for content rendering
│       │   ├── rating-buttons/        → Two-button Forgotten / Remembered rating UI
│       │   ├── card-explanation/      → Collapsible explanation panel shown after reveal; renders explanation Markdown via ngx-markdown
│       │   └── session-recovery-notice/ → Reusable warning banner for pending unsubmitted sessions
│       └── services/
│           ├── study.service.ts            → API calls for due cards and session submission
│           ├── session-backup.service.ts   → Persists and restores in-progress sessions via localStorage
│           └── study-state.service.ts      → Coordinates the navigation guard and deactivation confirmation flow
└── shared/             → Reusable UI components with no feature-specific logic
    └── components/
        ├── card-count-badges/  → New / due / relearning pill group
        ├── card-state-badge/   → FSRS state pill
        ├── card-type-badge/    → Card type pill (BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE)
        ├── confirm-dialog/     → Generic confirmation modal with configurable variant and labels
        ├── empty-state/        → Icon + title + subtitle + action slot for empty screens
        ├── filter-dropdown/    → Click-outside-aware dropdown for filter options
        ├── language-switcher/  → Language selection dropdown
        ├── page-header/        → Page title and subtitle header
        ├── pagination/         → Page number strip with gap logic
        └── toast/              → Fixed notification overlay
```

## Routing Pattern

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
    { path: 'decks',     loadComponent: () => import('./features/decks/decks.component') },
    { path: 'cards',     loadComponent: () => import('./features/card-list/card-list.component') },
    { path: 'study',     loadComponent: () => import('./features/study/study.component'), canDeactivate: [studySessionGuard] },
    { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component') },
    { path: 'profile',   loadComponent: () => import('./features/profile/profile.component') },
  ],
}

// login.component.ts
export default class LoginComponent { ... }
```

## State Management

Global state uses Angular **Signals** — no NgRx, no BehaviorSubjects:

- Private writable signal: `private readonly _x = signal<T>(null)`
- Public readonly surface: `readonly x = this._x.asReadonly()`
- Derived state: `readonly isX = computed(() => this._x() !== null)`

Services are injected via `inject()` (functional injection, no constructor parameters).

## Design Tokens

Three-layer token system in `styles.css`:

1. **Raw palette** — all Catppuccin Mocha/Latte color variables
2. **Semantic tokens** — theme-aware CSS custom properties (`--color-background`, `--color-primary`, etc.) swapped by `data-theme` attribute on `<html>`
3. **`@theme inline`** — Tailwind v4 block exposing semantic tokens as utility classes (`bg-background`, `text-primary`, etc.)

## i18n

Transloco v8 — translation files live in `public/i18n/{lang}.json`. Available languages: `en` (English), `es` (Spanish), `fr` (French), `pt` (Portuguese). Components use `TranslocoPipe` (`| transloco`), never the deprecated `TranslocoDirective` with `inlineRead`.
