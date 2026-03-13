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

The database schema, including tables for user management, content (decks/cards), and the FSRS algorithm (study progress and review logs), must be created manually for the initial setup.

1. In DBeaver, open an **SQL Editor** for the `reitera_db` connection.
2. Copy the contents of the initialization script (soon to be located in `backend/src/main/resources/init.sql`).
3. Execute the script to generate all necessary tables and relationships.
4. Refresh the `public` schema in DBeaver to verify that the 8 core tables (`users`, `roles`, `decks`, `cards`, `categories`, `tags`, `study_progress`, `review_logs`) and intermediary tables have been successfully created.
