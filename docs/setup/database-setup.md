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
| `roles` | RBAC roles (USER, ADMIN) |
| `users` | User accounts |
| `categories` | High-level groupings for decks |
| `tags` | Cross-deck labels for cards |
| `decks` | Flashcard collections |
| `cards` | Individual flashcards (JSONB answers) |
| `card_tags` | Many-to-many: cards ↔ tags |
| `study_progress` | FSRS state per (user, card) pair |
| `review_logs` | Immutable history of every review action |
| `user_deck_subscriptions` | Community deck subscriptions |

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
| `alumno@reitera.com` | `reitera2026` | USER |
| `admin@reitera.com` | `reitera2026` | ADMIN |

> Passwords are hashed at runtime using **pgcrypto's BCrypt** (`gen_salt('bf', 10)`), producing a hash compatible with Spring Security's `BCryptPasswordEncoder`.

### Demo content

| Resource | Count | Details |
|---|---|---|
| Categories | 3 | Historia de España · Programación Java · Inglés B2 |
| Tags | 4 | importante · difícil · repaso · vocabulario |
| Decks | 3 | One per category, owned by `alumno` |
| Cards | 16 | BASIC(7) · CLOZE(2) · MULTIPLE_CHOICE(4) · TRUE_FALSE(5) |
| StudyProgress | 6 | Cards from Deck 1, all states covered: Learning(2), Review(2), Relearning(2) |
| ReviewLogs | 14 | Historical review entries for the 6 progress records |

### FSRS states in the seed

The 6 `study_progress` records in Deck 1 (*La Segunda Guerra Mundial*) are configured so that **5 cards are immediately due** (past `next_review`) and **1 is scheduled for the future**. This means a call to `GET /study/due?deckId=1` will return those 5 cards plus the 10 new cards from Decks 2 and 3.

### Idempotency

The script raises an error if seed data is already present (`alumno@reitera.com` already exists). To reset and re-seed:

```sql
DELETE FROM review_logs;
DELETE FROM study_progress;
DELETE FROM card_tags;
DELETE FROM cards;
DELETE FROM decks;
DELETE FROM categories;
DELETE FROM tags;
DELETE FROM users WHERE email IN ('alumno@reitera.com', 'admin@reitera.com');
```

Then run `seed.sql` again.
