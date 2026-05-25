# Frontend Architecture

The Angular 21 frontend is a **Single-Page Application** using standalone components — no NgModules anywhere.

## Folder Structure

```
src/app/
├── core/               → Singleton services, guards, interceptors, and models (never imported by feature modules)
│   ├── auth/           → AuthService (login, register, logout, GET /users/me, token storage)
│   ├── guards/         → authGuard, noAuthGuard (functional CanActivateFn)
│   ├── interceptors/   → jwtInterceptor (attaches Bearer token; intercepts 401 to refresh and retry)
│   ├── models/         → TypeScript interfaces mapping all backend DTOs
│   ├── profile-panel/  → ProfilePanelService
│   ├── theme/          → ThemeService (dark/light, localStorage persistence, data-theme on <html>)
│   └── toast/          → ToastService
├── features/           → One folder per feature; each has its own routes file and components
│   ├── auth/           → auth.routes.ts, LoginComponent, RegisterComponent, CheckEmailComponent (/auth/check-email), VerifyComponent (/auth/verify)
│   │   └── services/
│   │       └── email-verification.service.ts → EmailVerificationService (verifyEmail, resendEmail)
│   ├── dashboard/      → DashboardComponent — stat badges, activity heatmap, last studied decks
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
│   ├── card-list/  → CardListComponent — cross-deck card list at /cards; filter bar (question, type, state, tag); edit/delete inline
│   │   └── services/
│   │       └── cards.service.ts → CardsService (getCards w/ filters, getTags, updateCard, deleteCard)
│   ├── profile/        → ProfileComponent — user info display, avatar, joined date, account details
│   ├── shell/          → AppShellComponent + all layout sub-components
│   │   ├── mobile-header/         → MobileHeaderComponent (hamburger, shown on small screens only)
│   │   └── sidebar/               → SidebarComponent (collapsible, desktop + mobile overlay)
│   │       ├── sidebar-header/    → SidebarHeaderComponent (logo + collapse toggle)
│   │       ├── sidebar-nav/       → SidebarNavComponent (nav links with icons)
│   │       └── sidebar-footer/    → SidebarFooterComponent (avatar, username, email, profile panel trigger)
│   │           └── profile-panel/ → ProfilePanelComponent (view profile, theme, language, logout)
│   └── study/          → StudyComponent (hub — deck picker, due counts, category study)
│       ├── components/
│       │   ├── study-hub/     → StudyHubComponent (deck list with card count badges)
│       │   ├── study-session/ → StudySessionComponent (card review flow, progress bar, confirm-dialog back guard)
│       │   └── study-card/    → StudyCardComponent (renders BASIC, MC, TF; reveals explanation)
│       └── services/
│           └── study.service.ts → StudyService (getDueCards, processSession)
└── shared/             → Reusable components with no feature-specific logic
    └── components/
        ├── card-count-badges/ → CardCountBadgesComponent (new/due/relearning pill group; used in study hub)
        ├── card-state-badge/  → CardStateBadgeComponent (FSRS state pill; input: state: number | null)
        ├── card-type-badge/   → CardTypeBadgeComponent (card type pill; input: type: CardType)
        ├── confirm-dialog/    → ConfirmDialogComponent
        ├── empty-state/       → EmptyStateComponent (icon + title + subtitle + action content projection)
        ├── language-switcher/ → LanguageSwitcherComponent
        ├── page-header/       → PageHeaderComponent (page title + subtitle)
        ├── pagination/        → PaginationComponent (page strip with gap logic; input: currentPage, totalPages)
        ├── filter-dropdown/   → FilterDropdownComponent (click-outside-aware dropdown; labelKey i18n or raw label; colorHex for tag pills)
        ├── tag-pill/          → TagPillComponent (colored pill; inputs: name, hexColor, removable; output: remove)
        └── toast/             → ToastComponent
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
    { path: 'study',     loadComponent: () => import('./features/study/study.component') },
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
