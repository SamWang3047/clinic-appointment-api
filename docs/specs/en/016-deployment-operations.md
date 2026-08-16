# 016 — Deployment, CI and Operations

**Status:** Approved\
**Priority:** P2\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines repeatable local startup, continuous integration,
container delivery, configuration and operational health.

## 2. Runtime profiles

- `local`: application plus Docker Compose PostgreSQL for development.
- `test`: automated configuration supplied by tests and Testcontainers.
- `demo`: local profile with explicitly enabled deterministic demonstration
  data.
- `prod`: environment-only secrets, no demo data and production-safe logging.

No profile contains committed real credentials.

## 3. Configuration

Environment configuration supplies at least:

- JDBC URL, username and password;
- JWT signing secret or key material;
- allowed CORS origins when a client is introduced;
- log level; and
- optional rate-limit and operational tuning values.

The application fails fast when required production configuration is missing.

## 4. Database lifecycle

- PostgreSQL is the supported database.
- Flyway migrations run before the application becomes ready.
- Hibernate uses schema validation and does not create or update production
  tables.
- Migrations are immutable after release; corrections use a new version.
- Backward compatibility is considered when a rolling deployment would overlap
  application versions, although the interview demo may deploy atomically.

## 5. Containers and local startup

- Docker Compose starts the application and PostgreSQL with health checks.
- The application container uses a Java 21 runtime, runs as a non-root user and
  contains no source secrets.
- PostgreSQL data uses a named local volume.
- Local setup documents start, health check, demo and shutdown commands.
- Development data removal uses an explicitly named local-only operation.

## 6. Continuous integration

GitHub Actions runs on pull requests and the main branch:

1. check out source;
2. configure Java 21;
3. run OpenAPI/spec validation;
4. run `./mvnw clean verify` with Testcontainers;
5. build the executable JAR;
6. build the Docker image; and
7. publish artifacts only from an authorised release workflow.

A failed required check prevents merge.

## 7. Health and observability

- Liveness reports whether the process can continue running.
- Readiness reports whether required dependencies, including PostgreSQL and
  migrations, are ready.
- Health responses expose no credentials, SQL or stack traces.
- Structured application logs are written to standard output for collection.
- Correlation IDs flow through request logs and error responses.

## 8. Demo data and interview path

- Demo data is deterministic and enabled only by explicit `demo` configuration.
- It includes accounts for each role, active doctors, availability, patients
  and appointments in useful lifecycle states.
- Demo credentials are clearly marked non-production.
- The README provides a five-minute flow: login, discover slots, create,
  conflict, confirm, authorisation denial, cancel/reschedule and audit evidence.

## 9. Recovery boundary

Formal production backup, restore, disaster recovery and retention schedules are
outside this interview release. The documentation must state that limitation
rather than implying production readiness.

## 10. Acceptance criteria

- A clean checkout can start through documented Docker Compose commands.
- Readiness becomes healthy only after PostgreSQL and migrations are ready.
- CI succeeds without a manually provisioned database.
- No production secret exists in Git or the Docker image.
- The demo profile cannot activate accidentally in the production profile.
