# Reitera

> Spaced repetition flashcard app powered by the FSRS-6 algorithm.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Angular](https://img.shields.io/badge/Angular-21-red?logo=angular)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-green?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)

## What is Reitera?

Reitera is a web application for learning with flashcards using spaced repetition. It implements the **FSRS-6** algorithm — the same science behind Anki — to schedule card reviews at the optimal moment before you forget them.

Cards support three types: basic (front/back), multiple choice, and true/false. Progress is tracked per user per card, so two people studying the same deck each have independent learning curves.

## Live demo

> **URL:** [https://reitera.vercel.app](https://reitera.vercel.app)

## Tech stack

| Layer      | Technology                                                                                   |
| ---------- | -------------------------------------------------------------------------------------------- |
| Frontend   | Angular 21 — standalone components, signals, Transloco i18n, Tailwind CSS                    |
| Backend    | Spring Boot 4.0.3 — stateless JWT auth (15 min access + 7 day refresh), Spring Security, JPA |
| Database   | PostgreSQL 16 — JSONB for card variants, manual schema management                            |
| Dev DB     | Docker Compose                                                                               |
| Deployment | Vercel (frontend) · Render (backend) · Supabase (database)                                   |

## Local development

### Prerequisites

- Node.js 22 LTS, Angular CLI 21
- Java 21, Maven 3.9+
- Docker (for the local database)

### Setup (3 steps)

**1. Start the database**

```bash
docker-compose up -d
# Then connect to psql and run:
# \i backend/reitera-backend/src/main/resources/init.sql
# \i backend/reitera-backend/src/main/resources/seed.sql
```

**2. Start the backend**

```bash
cd backend/reitera-backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

**3. Start the frontend**

```bash
cd frontend
ng serve
# Runs on http://localhost:4200
```

Full setup guide: [docs/setup/local-environment.md](docs/setup/local-environment.md)

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full breakdown of modules, security model, and design decisions.

## API docs

- Local Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Postman collection: [docs/api/reitera-postman-collection.json](docs/api/reitera-postman-collection.json)
- Endpoint docs: [docs/api/](docs/api/)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT — see [LICENSE](LICENSE).

## Author

**Helder Ruiz** — [manuhelderruiz@gmail.com](mailto:manuhelderruiz@gmail.com)

Started as a Higher Degree final project (DAW), developed into a fully-featured application.
