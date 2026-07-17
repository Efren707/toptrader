# Developer Setup Guide — Outline

> Status: Draft outline only — no prose yet. Scope: getting a **local** dev environment running (per [environments.md](../architecture/environments.md), ADR 0009). Deploying to prod is out of scope here — see [deployment-architecture.md](../architecture/deployment-architecture.md) for that.

## 1. Prerequisites
Tools a contributor needs installed before starting: JDK version (matching Spring Boot's requirement), Node/npm version (for Angular), Docker (for Postgres), and Git.

## 2. Cloning the repo
`git clone` the repo, brief note on the monorepo layout (backend/frontend in one repo, per ADR 0006).

## 3. Starting the local database
`docker compose up -d` to start the Dockerized Postgres container, and what image/version it uses (matching the RDS major version per ADR 0009/environments.md).

## 4. Backend config setup
Copy `application-local.yml.example` → `application-local.yml` (gitignored), and fill in the two secrets a developer must supply themselves: their own Finnhub API key and a session-signing value. Links to where to get a free Finnhub key.

## 5. Running the backend
`mvnw spring-boot:run` (or IDE run) with the `local` Spring profile, what port it comes up on (`8080`), and how Flyway migrations run automatically at startup.

## 6. Running the frontend
Install frontend deps, run the Angular dev server, what port it comes up on (`4200`), and that CORS is already configured for `localhost:4200` ↔ `localhost:8080` locally.

## 7. Verifying the setup works
A quick smoke check: hit `/actuator/health` on the backend, load the frontend in a browser, confirm they talk to each other (e.g. register a test account).

## 8. Running tests
How to run backend tests and frontend tests locally, matching what CI runs in the lint/test pipeline stages (ADR 0016).

## 9. Common local dev issues
Placeholder for gotchas that surface once someone actually follows these steps fresh (e.g. port conflicts, Docker not running, missing env values) — to be filled in after a real dry run.
