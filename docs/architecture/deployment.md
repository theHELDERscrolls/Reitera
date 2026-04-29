# CI Pipeline & Deployment

## CI Pipeline

A GitHub Actions workflow (`.github/workflows/ci.yml`) validates every push and pull request targeting `develop` or `main`. Two jobs run in parallel:

| Job                               | Steps                                                                                                                   |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **Backend — Build & Test**        | Checkout → JDK 21 (Temurin, Maven cache) → PostgreSQL 16 service container → apply `init.sql` → `mvn --batch-mode test` |
| **Frontend — Lint, Test & Build** | Checkout → Node 22 (npm cache) → `npm ci` → `ng lint` → `ng test --watch=false` → `ng build`                            |

**Why a real PostgreSQL, not H2?** The backend uses `hibernate.ddl-auto: none`, native JSONB columns, and `pgcrypto` — none of which H2 supports. The service container matches the exact production database engine and avoids a false sense of security from a divergent in-memory DB.

**Branch protection:** `develop` is configured to require both jobs to be green before any PR can be merged.

## Production Deployment

Reitera is deployed as three independent services connected over HTTPS:

```
Browser
  │
  ├─► Vercel (frontend — Angular SPA, static build, CDN)
  │
  └─► Render Web Service (backend — Spring Boot, Docker)
            │
            └─► Supabase PostgreSQL (managed PostgreSQL 16, Frankfurt)
```

| Service    | Provider             | Notes                                                                             |
| ---------- | -------------------- | --------------------------------------------------------------------------------- |
| Frontend   | Vercel               | Auto-deploys from `main`; output dir `dist/frontend/browser`                      |
| Backend    | Render (free tier)   | Docker runtime; `spring.profiles.active=prod`; spins down after 15 min inactivity |
| Database   | Supabase (free tier) | Direct connection port 5432; HikariCP pool capped at 5 connections                |
| Keep-alive | UptimeRobot          | Pings `/actuator/health` every 5 min to prevent Render cold starts                |

### Spring Boot profiles

`application.yml` holds shared defaults. Profile-specific files override per environment:

| File                   | Activated by                                                 | Purpose                                               |
| ---------------------- | ------------------------------------------------------------ | ----------------------------------------------------- |
| `application-dev.yml`  | default (`spring.profiles.active: dev` in `application.yml`) | Local Docker DB, dev JWT secret, Swagger on           |
| `application-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` env var on Render              | All secrets via env vars, Swagger off, HikariCP tuned |

### CORS

Allowed origins are read from `app.cors.allowed-origins` (comma-separated string), injected via `@Value` in `SecurityConfig`. In dev: `http://localhost:4200`. In prod: the Vercel domain, set via `CORS_ALLOWED_ORIGINS` environment variable in Render.

### Health check

`GET /actuator/health` — public endpoint (no auth required). Returns `{"status":"UP"}` when the application and database connection are healthy. Configured as the Render health check path and the UptimeRobot monitor URL.
