# 014 — Non-Functional Requirements

**Status:** Approved\
**Priority:** P1\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines measurable quality constraints for the interview
release without presenting them as a production service-level agreement.

## 2. Correctness and reliability

- The application starts only when Flyway migrations and JPA schema validation
  succeed.
- No appointment command may leave a partial business change or misleading
  success audit after rollback.
- UTC is used for stored appointment instants and an injected `Clock` is used
  for time-dependent business decisions.
- Melbourne local schedules use `ZoneId` rules rather than a fixed offset.
- Retried and concurrent commands follow Spec 012.

## 3. Performance baseline

The interview release records, but does not promise as a production SLA, a
repeatable one-minute baseline using:

- 50 doctors;
- 1,000 patients;
- 10,000 appointments; and
- 20 concurrent users.

The baseline reports p50, p95, throughput and error rate for doctor listing,
available-slot calculation, appointment listing and appointment creation. The
environment and commit are recorded with results.

Timing assertions do not block ordinary CI because shared runners are variable.
Functional performance checks still enforce pagination, database-side filters,
appropriate indexes and absence of known N+1 query paths.

## 4. Capacity and request limits

- Pageable collections default to 20 and allow at most 100 items.
- JSON request bodies are limited to 1 MiB in the first release.
- Appointment reason is limited to 500 characters.
- List filtering operates in PostgreSQL rather than loading the full data set.
- Frequently filtered appointment columns have migration-managed indexes.

## 5. Security quality

- Secrets come from environment configuration and are absent from source,
  images and logs.
- Deployed login and API traffic use HTTPS at the platform boundary.
- Login failures are rate-limited with a configurable policy; the demo default
  is five failed attempts per minute per normalised email and client address.
- Security tests cover every role and ownership boundary.
- Dependencies are reviewed by CI tooling when available.

## 6. Observability

- Every request has a correlation ID.
- Structured logs contain method, route template, status, duration and
  correlation ID but no sensitive body.
- Health, liveness and readiness endpoints are available to operators.
- Readiness reflects required database connectivity and migration completion.
- Metrics may expose request counts, latency and errors without resource-level
  sensitive labels.

## 7. Maintainability

- Controllers handle HTTP concerns; services own use cases and transactions;
  entities protect state invariants; repositories own persistence queries.
- Constructor injection is used for required collaborators.
- API entities are isolated from persistence entities by DTOs.
- Schema changes use additive, versioned Flyway migrations.
- Approved specifications and tests are updated before implementation changes.

## 8. Portability and repeatability

- The project builds with Java 21 and the committed Maven Wrapper.
- Automated tests run with Testcontainers PostgreSQL and no developer-specific
  database state.
- Docker Compose provides a repeatable local application and database setup.
- Configuration differences are expressed through profiles and environment
  variables, not source edits.

## 9. Data retention

- Doctor, patient and appointment business records are not physically deleted
  in the first release.
- Status changes and deactivation preserve history.
- Audit records are append-only and have no automatic production purge in the
  interview release.
- Formal backup, archive and retention periods are deployment decisions outside
  this release.
- A clearly scoped development-only mechanism may reset demo data.

## 10. Quality gate

- `./mvnw clean verify` passes.
- OpenAPI syntax validation passes.
- English specifications and code contain no committed secrets.
- Docker image construction succeeds.
- The documented demo can be repeated from a clean checkout.
