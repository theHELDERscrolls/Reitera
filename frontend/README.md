# Frontend — Reitera

Angular 21 SPA for the Reitera spaced repetition flashcard application. Uses standalone components, Angular Signals for state management, Transloco for i18n (EN / ES / FR / PT), and Tailwind CSS v4 with the Catppuccin colour theme.

## Requirements

- Node.js 22 LTS
- npm 10+
- Angular CLI 21 (`npm install -g @angular/cli`)

## Development server

```bash
npm install
ng serve
# App available at http://localhost:4200
```

Requires the backend running at `http://localhost:8080`. See [docs/setup/local-environment.md](../docs/setup/local-environment.md) for the full stack setup.

## Commands

```bash
ng serve    # dev server on http://localhost:4200
ng build    # production build → dist/
ng test     # unit tests (Vitest + jsdom)
ng lint     # ESLint
```

## Environments

| File | Used when | API URL |
|---|---|---|
| `src/environments/environment.development.ts` | `ng serve` (default) | `http://localhost:8080/api/v1` |
| `src/environments/environment.ts` | `ng build` (production) | `https://reitera-backend.onrender.com/api/v1` |

## Architecture

See [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for the full frontend folder structure, routing pattern, state management conventions, and design token system.
