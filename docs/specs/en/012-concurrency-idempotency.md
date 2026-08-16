# 012 — Concurrency and Idempotency

**Status:** Approved\
**Priority:** P1\
**Last updated:** 2026-08-16

## 1. Purpose

This specification prevents overlapping appointments, lost updates and
duplicate commands when requests arrive concurrently or are retried.

## 2. Transaction boundaries

- Appointment creation, confirmation, decline, cancellation, rescheduling and
  completion each execute in an explicit service-layer transaction.
- Rescheduling validates availability, updates the appointment and appends
  history atomically.
- Business changes and their success audit event commit or roll back together.
- Read operations use read-only transactions where a persistence context is
  required.

## 3. Isolation and lock order

- PostgreSQL `READ_COMMITTED` is the default isolation level.
- Creation and rescheduling lock the doctor, then the patient, using database
  pessimistic write locks.
- Every use case that acquires both follows the fixed `Doctor → Patient` order.
- Overlap queries execute after those locks and inside the same transaction.
- The current appointment is excluded from its own reschedule overlap query.

This serialises competing requests for the same doctor or patient without
raising the isolation level for the entire application.

## 4. Active overlap

Only `PENDING` and `CONFIRMED` reserve time. Conflict detection applies the
half-open interval rule to both doctor and patient:

```text
existing.start < requested.end
AND existing.end > requested.start
```

Adjacent intervals are allowed. `DECLINED`, `CANCELLED` and `COMPLETED` do not
block a future slot.

## 5. Optimistic locking

- Mutable appointment state has a JPA `@Version` value.
- A stale confirmation, cancellation, completion or reschedule fails rather
  than overwriting a newer state.
- Optimistic lock failures map to `409 CONCURRENT_MODIFICATION`.
- Version values are internal and not returned in public DTOs.

## 6. Idempotency key

The following requests require an `Idempotency-Key` header:

- patient and receptionist appointment creation;
- confirm and decline;
- cancel and reschedule; and
- complete.

The key is a UUID supplied by the client and is scoped to authenticated account
and operation. Records are retained for 24 hours.

## 7. Idempotency record

An idempotency record contains:

```text
accountId
operation
idempotencyKey
requestHash
state
httpStatus
responseBody
resourceId
createdAt
expiresAt
```

A database unique constraint covers `(accountId, operation, idempotencyKey)`.
The request hash is computed from a canonical representation and does not expose
the plaintext reason in logs.

## 8. Replay behaviour

- First use reserves the key and executes the operation.
- Same key and same request returns the stored final response without repeating
  the business action or audit event.
- Same key and different request returns `409 IDEMPOTENCY_KEY_REUSED`.
- A concurrent duplicate while the first request is incomplete returns
  `409 IDEMPOTENCY_REQUEST_IN_PROGRESS` and may include `Retry-After`.
- Retried requests after completion replay the stored response.
- Final 2xx and deterministic 4xx/409 outcomes may be stored; unexpected 5xx
  outcomes are not replayed as permanent results.

## 9. Database constraints and failures

- Normalised account and patient emails have unique constraints.
- The idempotency scope has a unique constraint.
- Database constraints are final race-condition protection even when an
  application pre-check exists.
- Lock timeout, deadlock and constraint failures are translated to stable
  conflict responses and logged with correlation ID.

## 10. Acceptance scenarios

- Two concurrent bookings for one doctor/time produce one success and one
  conflict.
- Two concurrent bookings for one patient/time produce one success and one
  conflict.
- Adjacent bookings both succeed.
- Concurrent state updates cannot overwrite each other.
- Retrying a completed command with the same key returns the same response and
  creates no duplicate audit event.
- Reusing a key with different JSON returns the documented conflict.
- A failed reschedule leaves the appointment and history unchanged.
