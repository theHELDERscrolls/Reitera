# Database Setup

This document outlines the steps required to set up the local PostgreSQL database for the **Reitera** project using Docker and DBeaver.

## Prerequisites

Ensure you have the following tools installed on your local machine:

* [Docker](https://docs.docker.com/get-docker/) and Docker Compose.
* [LazyDocker](https://github.com/jesseduffield/lazydocker) (Highly recommended for container monitoring).
* [DBeaver](https://dbeaver.io/) or any preferred PostgreSQL client.

## 1. Starting the Database Container

The project uses a `docker-compose.yml` file located at the **root** of the monorepo to manage infrastructure services.

1. Open your terminal and navigate to the root directory of the project.
2. Run the following command to start the PostgreSQL container in detached mode:

```bash
docker compose up -d
```

3. *(Optional)* Open **LazyDocker** by typing `lazydocker` in your terminal to verify that the `reitera_postgres` container is running and healthy.

## 2. Connecting via DBeaver

Once the container is up, configure your database client to connect to it.

1. Open **DBeaver** and create a new **PostgreSQL** connection.
2. Enter the following connection details:
   * **Host:** `localhost`
   * **Port:** `5432`
   * **Database:** `reitera_db`
   * **Username:** `reitera_admin`
   * **Password:** `reitera_password`
3. Click **Test Connection** (download drivers if prompted) and then **Finish**.

## 3. Initializing the Database Schema

The database schema must be created manually before starting the Spring Boot application. The project uses `ddl-auto: none`, meaning Hibernate will **not** auto-create or modify tables.

The schema script is located at:

```
backend/reitera-backend/src/main/resources/init.sql
```

**Steps:**

1. In DBeaver, open an **SQL Editor** for the `reitera_db` connection.
2. Open `init.sql` and execute its contents.
3. Refresh the `public` schema in DBeaver to verify that the following tables were created:

| Table | Description |
|---|---|
| `roles` | RBAC roles (STUDENT, ADMIN) |
| `users` | User accounts |
| `categories` | High-level groupings for decks |
| `decks` | Flashcard collections |
| `cards` | Individual flashcards (JSONB answers) |
| `study_progress` | FSRS state per (user, card) pair |
| `review_logs` | Immutable history of every review action |
| `refresh_tokens` | Active user sessions (SHA-256 hashed tokens, expiry, revoked flag) |

> **Note:** `init.sql` enables the `pgcrypto` extension, which is required by `seed.sql` to generate BCrypt password hashes at runtime.

## 4. Loading Demo Seed Data

After initializing the schema, you can populate the database with realistic demo data for local development and TFG demonstrations.

The seed script is located at:

```
backend/reitera-backend/src/main/resources/seed.sql
```

**Steps:**

1. In DBeaver, open a new **SQL Editor** for the `reitera_db` connection.
2. Open `seed.sql` and execute its contents.
3. The script will populate the database with the following data:

### Demo users

| Email | Password | Role |
|---|---|---|
| `alumno@reitera.com` | `reitera2026` | STUDENT |

> Passwords are hashed at runtime using **pgcrypto's BCrypt** (`gen_salt('bf', 10)`), producing a hash compatible with Spring Security's `BCryptPasswordEncoder`.

### Demo content

| Resource | Count | Details |
|---|---|---|
| Categories | 3 | Historia de España · Programación Java · Inglés B2 |
| Decks | 4 | Two under "Historia de España" (enables category-scoped study demo), one under "Programación Java", one under "Inglés B2" |
| Notes | 20 | Source content; auto-generates child cards via `NoteParser` |
| Cards | 20+ | Generated from notes; types: BASIC · BASIC_REVERSE · CLOZE · MULTIPLE_CHOICE (BASIC_REVERSE and CLOZE notes produce multiple cards each) |
| StudyProgress | 10 | Deck 1: 6 records (Learning, Review, Relearning); Decks 2 & 3: 2 records each |
| ReviewLogs | 20 | Historical review entries for all 10 progress records |

### FSRS states in the seed

The 10 `study_progress` records are distributed across three decks: Deck 1 (*La Segunda Guerra Mundial*) has 6 records with **5 cards immediately due** and 1 scheduled for the future; Decks 2 and 3 have 2 records each with 1 due card each. New cards (no prior progress) fill the remainder of each session. A call to `GET /study/due?deckId=1` returns the 5 overdue cards plus 6 new cards from that deck.

### Idempotency

The script raises an error if seed data is already present (`alumno@reitera.com` already exists). To reset and re-seed, run `truncate.sql` first:

```
backend/reitera-backend/src/main/resources/truncate.sql
```

Then run `seed.sql` again.
