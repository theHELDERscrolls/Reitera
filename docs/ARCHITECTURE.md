# Architecture Overview

Reitera follows a **monorepo structure** with two independent applications — a Spring Boot REST API and an Angular SPA — backed by a PostgreSQL database. The backend exposes a stateless REST API consumed by the frontend. There is no server-side rendering.

```
Reitera/
├── backend/    → Spring Boot REST API (Java 21)
├── frontend/   → Angular SPA (TypeScript)
└── docs/       → Project documentation
```

## Documents

| Document | Contents |
|---|---|
| [architecture/backend.md](architecture/backend.md) | Layer architecture, module structure, database design, study session batch flow |
| [architecture/frontend.md](architecture/frontend.md) | Folder structure, routing pattern, signals-based state management, design tokens, i18n |
| [architecture/security.md](architecture/security.md) | Two-token auth model, IDOR prevention, rate limiting, security headers, input constraints |
| [architecture/deployment.md](architecture/deployment.md) | GitHub Actions CI pipeline, production services (Vercel · Render · Supabase), Spring profiles |
