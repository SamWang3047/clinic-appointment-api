# 001 — Domain Model and Appointment Lifecycle

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines the core entities, relationships, invariants and
appointment state transitions. It describes observable domain behaviour rather
than a particular JPA implementation.

## 2. Core entities

### Doctor

- Has a system-generated UUID, full name, specialty and active flag.
- Has zero or more recurring working-hour intervals and date overrides.
- A deactivated doctor remains in historical appointments but cannot receive a
  new appointment or be shown as available.

### Patient

- Has a system-generated UUID, full name, email and phone number.
- Email is trimmed, case-normalised and unique.
- A patient can access only appointments owned by that patient unless another
  role-specific rule explicitly grants access.

### Appointment

- Has one doctor and one patient.
- Has a UTC start instant and end instant.
- Has a duration of exactly 15, 30, 45 or 60 minutes.
- Has a required, sensitive appointment reason.
- Has one current status.
- Records creation and update timestamps.
- Records who performed confirmation, decline, cancellation and rescheduling
  actions when an authenticated actor performs them.
- Retains a history of state and schedule changes.

### Doctor availability

- A doctor owns recurring weekly working-hour intervals.
- Date-specific overrides replace the recurring intervals for that local date.
- Availability is interpreted in the `Australia/Melbourne` time zone and
  exposed as UTC instants at the API boundary.

## 3. Appointment states

| Status | Meaning | Reserves time |
| --- | --- | --- |
| `PENDING` | Requested and awaiting a decision | Yes |
| `CONFIRMED` | Accepted by a doctor or receptionist | Yes |
| `DECLINED` | Rejected before confirmation | No |
| `CANCELLED` | Cancelled after creation | No |
| `COMPLETED` | Consultation completed | No future availability impact |

`BOOKED` and `NO_SHOW` are not part of the first-release state model.

## 4. State transitions

| From | Action | To | Permitted actor |
| --- | --- | --- | --- |
| — | Create | `PENDING` | Patient for self; receptionist for a patient |
| `PENDING` | Confirm | `CONFIRMED` | Assigned doctor or receptionist |
| `PENDING` | Decline | `DECLINED` | Assigned doctor or receptionist |
| `PENDING` | Cancel | `CANCELLED` | Owning patient or authorised staff |
| `CONFIRMED` | Cancel | `CANCELLED` | Owning patient or authorised staff |
| `CONFIRMED` | Complete | `COMPLETED` | Assigned doctor |

Terminal states are `DECLINED`, `CANCELLED` and `COMPLETED`. A terminal
appointment cannot transition to another status.

## 5. Creation invariants

- The doctor and patient must exist.
- The doctor must be active.
- The start must be in the future.
- The end must be after the start.
- The duration must be 15, 30, 45 or 60 minutes.
- The start must lie on a 15-minute boundary.
- The complete interval must fit within one doctor working interval for the
  relevant Melbourne local date.
- The reason must not be blank.
- The new appointment starts as `PENDING`.
- It must not overlap another `PENDING` or `CONFIRMED` appointment for the
  doctor or patient.

Two half-open intervals overlap when:

```text
existing.start < requested.end
AND existing.end > requested.start
```

Adjacent appointments are therefore allowed.

## 6. Confirmation and decline invariants

- Only `PENDING` appointments can be confirmed or declined.
- The appointment must not have started.
- A declined appointment no longer reserves its time.
- Pending appointments do not expire automatically in the first release.

## 7. Cancellation invariants

- Only `PENDING` or `CONFIRMED` appointments can be cancelled.
- A patient may cancel their own appointment no later than two hours before its
  start.
- Authorised staff may cancel until the appointment starts.
- Staff must provide a non-blank cancellation reason.
- A cancellation reason is optional for a patient.
- Cancellation releases the reserved time but does not delete history.

## 8. Rescheduling invariants

- Rescheduling is an atomic change to the existing appointment and retains its
  UUID.
- It applies the same duration, availability and conflict rules as creation.
- A patient rescheduling a `CONFIRMED` appointment returns it to `PENDING`.
- An authorised doctor or receptionist may retain `CONFIRMED` after
  rescheduling.
- Every successful reschedule records the old interval, new interval, actor and
  timestamp in audit history.
- A failed reschedule leaves the original appointment unchanged.

## 9. Completion invariants

- Only a `CONFIRMED` appointment can be completed.
- Only the assigned doctor can complete it.
- It cannot be completed before its start time.

## 10. Concurrency requirement

Creation, confirmation, cancellation and rescheduling must execute within
explicit transaction boundaries. Concurrent requests must not create active
overlaps or silently overwrite a state change. The locking and idempotency
strategy is defined in Spec 012.

## 11. Acceptance summary

- A valid request inside availability creates a `PENDING` appointment.
- A request outside working hours is rejected.
- A request overlapping a pending or confirmed appointment is rejected.
- An adjacent request is accepted.
- A doctor or receptionist can confirm or decline a pending appointment.
- A patient cannot cancel inside the two-hour deadline.
- An authorised staff member can cancel before the start with a reason.
- A patient reschedule returns a confirmed appointment to pending.
- A failed reschedule preserves the original schedule.
- Terminal appointments reject further state transitions.
