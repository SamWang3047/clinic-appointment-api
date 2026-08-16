# 013 — Acceptance Test Catalogue

**Status:** Approved\
**Priority:** P1\
**Last updated:** 2026-08-16

## 1. Purpose

This specification provides traceability from approved behaviour to automated
tests. It defines outcomes, not a mandatory test framework or class layout.

## 2. Test layers

1. **Domain/unit:** entities, slot calculations and services with a fixed
   `Clock`; no Spring context.
2. **MVC/security:** request validation, JSON, status, authentication, ownership
   and field visibility.
3. **PostgreSQL integration:** Flyway, repositories, locking and transactions
   against Testcontainers PostgreSQL.
4. **Concurrency acceptance:** coordinated threads exercising real database
   locks and idempotency constraints.

The full suite runs with `./mvnw clean verify` and does not require a manually
started developer database.

## 3. Scenario format

Each scenario has a stable ID and Given/When/Then description. Tests include the
ID in a display name, tag or comment so a reviewer can trace failures to this
catalogue.

## 4. Registration and identity

| ID | Scenario |
| --- | --- |
| `PAT-001` | Valid self-registration creates one linked account and profile |
| `PAT-002` | Case-insensitive duplicate email returns `409` |
| `PAT-003` | Receptionist creates a profile without credentials |
| `PAT-004` | Invalid registration returns all safe field violations |
| `AUTH-001` | Valid login returns a 30-minute JWT |
| `AUTH-002` | Unknown email and wrong password return the same failure |
| `AUTH-003` | Disabled account cannot use an issued token |

## 5. Doctor and availability

| ID | Scenario |
| --- | --- |
| `DOC-001` | Administrator creates a linked doctor profile and account |
| `DOC-002` | Public list excludes inactive doctors and private fields |
| `DOC-003` | Deactivation preserves history and prevents new booking |
| `AVL-001` | Weekly intervals generate starts on a 15-minute grid |
| `AVL-002` | A slot never crosses a break or working-day boundary |
| `AVL-003` | Date override replaces recurring availability |
| `AVL-004` | Pending and confirmed appointments remove overlapping slots |
| `AVL-005` | Terminal appointments do not block slots |
| `AVL-006` | Public slots expose no occupancy reason or patient identity |

## 6. Appointment lifecycle

| ID | Scenario |
| --- | --- |
| `APT-001` | Patient creates a pending appointment without patient ID input |
| `APT-002` | Receptionist creates a pending appointment for a patient |
| `APT-003` | Invalid duration or off-grid start returns `400` |
| `APT-004` | Outside-availability request is rejected |
| `APT-005` | Doctor overlap returns `409` |
| `APT-006` | Patient overlap returns `409` |
| `APT-007` | Assigned doctor or receptionist confirms pending |
| `APT-008` | Assigned doctor or receptionist declines pending and frees time |
| `APT-009` | Terminal state rejects another transition |
| `APT-010` | Assigned doctor completes confirmed after start |

## 7. Cancellation and rescheduling

| ID | Scenario |
| --- | --- |
| `CHG-001` | Patient cancels more than two hours before start |
| `CHG-002` | Patient cancellation inside two hours returns `409` |
| `CHG-003` | Staff cancellation requires a reason |
| `CHG-004` | Successful cancellation frees time and appends audit |
| `CHG-005` | Valid reschedule retains appointment ID |
| `CHG-006` | Patient reschedule returns confirmed to pending |
| `CHG-007` | Staff reschedule may preserve confirmed |
| `CHG-008` | Failed reschedule leaves schedule and history unchanged |

## 8. Authorisation and privacy

| ID | Scenario |
| --- | --- |
| `SEC-001` | Anonymous protected request returns `401` |
| `SEC-002` | Wrong role on a capability returns `403` |
| `SEC-003` | Unrelated patient appointment lookup returns concealed `404` |
| `SEC-004` | Unrelated doctor cannot view or confirm appointment |
| `SEC-005` | Administrator appointment response omits reason |
| `SEC-006` | Logs and audit contain no password, token or clinical reason |

## 9. Concurrency and idempotency

| ID | Scenario |
| --- | --- |
| `CON-001` | Concurrent same-doctor booking produces one success |
| `CON-002` | Concurrent same-patient booking produces one success |
| `CON-003` | Stale state command returns concurrent-modification conflict |
| `IDEM-001` | Same key and request replays one result and one audit event |
| `IDEM-002` | Same key with different request returns `409` |
| `IDEM-003` | In-progress duplicate creates no second business operation |

## 10. Error contract

| ID | Scenario |
| --- | --- |
| `ERR-001` | Field validation returns Problem Details and violations |
| `ERR-002` | Malformed JSON exposes no internal parser detail |
| `ERR-003` | Unknown error returns safe `500` with correlation ID |
| `ERR-004` | All protected failures use the documented status and error code |

## 11. Completion criteria

- Every approved rule maps to at least one scenario.
- Every scenario is automated or explicitly marked manual with a reason.
- Success, boundary and failure cases are represented.
- Tests assert sensitive fields are absent, not merely that allowed fields are
  present.
- The suite is repeatable locally and in CI.
