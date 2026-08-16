# Implementation Gap Analysis

**Compared baseline:** commit `36dbc73` plus the current working tree\
**Target:** approved Specs 000–016 and `openapi/clinic-api.yaml`\
**Analysed:** 2026-08-16

## 1. Current source baseline

The current source tree contains the Spring Boot scaffold, PostgreSQL/Flyway
configuration, one migration, and JPA entities/repositories for Doctor, Patient
and Appointment.

The source currently has no controllers, services, API DTOs, security layer,
availability model, idempotency storage or audit-event model. Compiled classes
from an earlier uncommitted API iteration still exist under ignored `target/`,
but their source files are not present. A clean build will remove those stale
artifacts, so they are not counted as implementation.

## 2. Existing foundations that can be retained

- Java 21, Maven Wrapper and Spring Boot structure.
- PostgreSQL datasource and Flyway ownership of schema.
- `ddl-auto=validate`, UTC Hibernate JDBC time zone and disabled Open Session in
  View.
- UUID entity identifiers.
- Doctor and Patient core profile fields.
- Appointment doctor/patient relationships, UTC interval, reason and `@Version`.
- Audit timestamps through `AuditableEntity`.
- Initial appointment indexes and foreign-key restrictions.
- Actuator dependency and basic health exposure.

These foundations still require migration and behaviour changes before they
satisfy the target specs.

## 3. Material contract gaps

| Area | Current source | Approved target |
| --- | --- | --- |
| Appointment status | `BOOKED`, `CANCELLED`, `COMPLETED` | `PENDING`, `CONFIRMED`, `DECLINED`, `CANCELLED`, `COMPLETED` |
| Appointment duration | Arbitrary valid start/end | Server-derived 15/30/45/60 minutes on a 15-minute grid |
| Availability | None | Weekly intervals, breaks, date overrides and slot calculation |
| Accounts | None | Separate BCrypt-backed account with one role and profile link |
| Authentication | None | 30-minute JWT and protected/public endpoint boundary |
| Authorisation | None | Role plus patient ownership and doctor assignment |
| HTTP API | No source controllers | 21 OpenAPI paths with self-service and staff boundaries |
| DTO/privacy | No source DTOs | Role-aware DTOs and sensitive-field minimisation |
| Lifecycle | Entity supports only cancel | Confirm, decline, cancel, reschedule and complete rules |
| Audit | Created/updated timestamps only | Append-only actor/action history |
| Concurrency | `@Version` only | Fixed pessimistic lock order plus optimistic state protection |
| Idempotency | None | 24-hour account/operation/key records and replay |
| Errors | None in source | Stable Problem Details, codes and correlation IDs |
| Lists | None | Filtered, stable and database-paginated role views |
| Tests | Context and mapping tests | Unit, MVC/security, Testcontainers and concurrency acceptance |
| Delivery | Local database configuration | Compose app/database, CI, image and demo profile |

## 4. Required database evolution

Do not edit `V1__create_core_tables.sql`. Add forward Flyway migrations for:

- appointment status constraint/data transition from `BOOKED` to the approved
  state model;
- account table, role/status constraints and profile links;
- doctor weekly intervals and date overrides;
- appointment cancellation metadata and any actor references needed by the
  model;
- append-only audit events;
- idempotency records and their unique scope; and
- indexes supporting active overlap, date filtering, account lookup and audit
  pagination.

Migration sequencing must leave every intermediate schema valid for Hibernate
validation.

## 5. Recommended implementation PR sequence

### PR 1 — Specification baseline

- Commit English specs, OpenAPI and Chinese ignore rule.
- Add OpenAPI syntax/reference validation to the build or CI.
- Make no claim that the implementation already satisfies the contract.

### PR 2 — API and test foundation

- Add injected `Clock` and `Australia/Melbourne` zone configuration.
- Add Problem Details, correlation ID and stable domain exceptions.
- Introduce Testcontainers PostgreSQL and shared integration-test support.
- Add contract-focused MVC test conventions.

### PR 3 — Accounts, profiles and Spring Security

- Add account migration/entity/repository and BCrypt password service.
- Implement patient self-registration and receptionist-created profiles.
- Implement administrator-created staff/doctor accounts.
- Add JWT login, role rules, ownership helpers and security tests.

### PR 4 — Appointment lifecycle and audit model

- Migrate status model and enrich Appointment behaviour.
- Add cancellation/reschedule metadata and append-only audit events.
- Implement and unit-test legal and illegal state transitions.

### PR 5 — Doctor availability

- Add recurring intervals and date overrides.
- Implement Melbourne-to-UTC slot calculation with a fixed Clock.
- Add public slot API and availability-management authorisation.

### PR 6 — Appointment commands and queries

- Implement patient and receptionist creation flows.
- Implement confirm, decline, cancel, reschedule and complete commands.
- Add role-aware detail/list DTOs, filters, pagination and N+1-safe queries.
- Cover all relevant acceptance IDs before marking Specs 005–007 implemented.

### PR 7 — Concurrency and idempotency

- Add fixed `Doctor → Patient` pessimistic locking.
- Add idempotency persistence, hashing, replay and expiry behaviour.
- Add real PostgreSQL concurrent-booking and stale-update tests.

### PR 8 — Delivery and demonstration

- Add Docker image and complete Compose setup.
- Add GitHub Actions quality gates and OpenAPI validation.
- Add deterministic demo profile, performance baseline and five-minute README
  walkthrough.

## 6. Spec completion rule

A catalogue entry changes from `Approved` to `Implemented` only when:

- its schema migration is present where required;
- implementation conforms to OpenAPI;
- mapped acceptance scenarios pass;
- security and sensitive-field assertions pass; and
- `./mvnw clean verify` succeeds from a clean environment.
