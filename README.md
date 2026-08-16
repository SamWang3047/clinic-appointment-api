# Clinic Appointment API

An interview-focused Spring Boot API that demonstrates a secure medical
appointment workflow with PostgreSQL, Flyway, JPA, JWT authentication,
role-aware DTOs and integration tests backed by Testcontainers.

## Implemented interview slice

- Patient, doctor, receptionist and administrator accounts with BCrypt and
  30-minute JWT access tokens.
- Public doctor discovery and public slot calculation in
  `Australia/Melbourne`, returned as UTC instants.
- Role-protected recurring weekly doctor hours.
- Patient self-booking and receptionist booking for an existing patient.
- `PENDING → CONFIRMED` and active appointment cancellation rules.
- Doctor and patient overlap protection with fixed-order pessimistic locks.
- Optimistic locking for appointment state changes.
- Patient/doctor/receptionist/admin appointment queries with database
  pagination and role-specific field minimisation.
- Append-only, reason-free audit events committed with appointment mutations.
- RFC 9457-style problem responses with safe correlation IDs.

The approved OpenAPI and behavioural specifications under `docs/specs/en` are
the target contract. This time-boxed interview slice deliberately defers
date-specific availability overrides, durable idempotency replay,
decline/reschedule/complete HTTP commands, notifications and the admin audit
endpoint.

## Run locally

Requirements: Java 21 and Docker Desktop.

```powershell
docker compose up -d
$env:JWT_SECRET = "replace-with-at-least-32-random-characters"
$env:DEMO_PASSWORD = "choose-a-demo-password"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The `demo` profile idempotently creates these accounts using the password from
`DEMO_PASSWORD`:

- `patient@demo.local`
- `doctor@demo.local`
- `receptionist@demo.local`
- `admin@demo.local`

It also creates one active general practitioner with Monday–Friday
09:00–17:00 weekly hours. Demo credentials are never present in source control.

In another PowerShell terminal:

```powershell
.\demo\interview-demo.ps1
```

See [the interview walkthrough](docs/demo/INTERVIEW-DEMO.md) for the narrative
and likely follow-up questions.

## Verify

```powershell
.\mvnw.cmd clean verify
```

The integration suite starts an isolated PostgreSQL 16 container, applies all
Flyway migrations, validates Hibernate mappings, and exercises authentication,
availability, booking, conflict, privacy and audit behaviour.

## Key endpoints

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `POST /api/v1/auth/login` | Public | Obtain a JWT |
| `GET /api/v1/doctors` | Public | List active doctors |
| `GET /api/v1/doctors/{id}/available-slots` | Public | Calculate safe public slots |
| `PUT /api/v1/doctors/{id}/availability/weekly` | Doctor/staff | Replace recurring hours |
| `POST /api/v1/me/appointments` | Patient | Book for the authenticated patient |
| `GET /api/v1/me/appointments` | Patient/doctor | List the caller's appointments |
| `POST /api/v1/appointments` | Receptionist | Book for an existing patient |
| `GET /api/v1/appointments` | Receptionist/admin | Search the clinic schedule |
| `GET /api/v1/appointments/{id}` | Authorised roles | Read a role-filtered detail |
| `POST /api/v1/appointments/{id}/confirm` | Assigned doctor/receptionist | Confirm a pending request |
| `POST /api/v1/appointments/{id}/cancel` | Owner/authorised staff | Cancel under role-specific rules |

## Package structure

The code is organised by feature (`account`, `doctor`, `patient`,
`availability`, `appointment`) with shared infrastructure kept small. REST
controllers accept validated DTOs, transactional services enforce use-case
rules, entities protect aggregate invariants, and repositories own persistence
queries and locks.
