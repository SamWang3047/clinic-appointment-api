# 007 — Cancel and Reschedule Appointment

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines cancellation and atomic rescheduling without
deleting appointment history.

## 2. Cancellation permissions

- A patient may cancel their own `PENDING` or `CONFIRMED` appointment.
- The assigned doctor, a receptionist or an administrator may cancel an active
  appointment for clinic operations.
- A patient may cancel only when the start is at least two hours after the
  current instant.
- Authorised staff may cancel until, but not after, the start instant.
- Terminal appointments cannot be cancelled again.

## 3. Cancellation reason

- A staff cancellation requires a non-blank reason.
- A patient cancellation reason is optional.
- The reason is sensitive and follows the same visibility rules as the
  appointment reason.
- Successful cancellation records actor, role and timestamp.

## 4. Cancellation result

- Status becomes `CANCELLED`.
- The previously reserved interval becomes available.
- The appointment and its history remain stored.
- A repeated or invalid cancellation returns `409` and causes no further
  change.

## 5. Rescheduling permissions

- A patient may reschedule their own `PENDING` or `CONFIRMED` appointment.
- The assigned doctor or a receptionist may reschedule an active appointment.
- An administrator does not reschedule appointments in the first release.
- Terminal appointments cannot be rescheduled.
- Rescheduling is rejected after the appointment has started.

## 6. Rescheduling rules

- The request contains a new UTC start instant and allowed duration.
- The appointment UUID and original creation timestamp remain unchanged.
- The service applies all creation, doctor availability and overlap rules.
- Conflict checking excludes the appointment being rescheduled.
- A patient rescheduling a confirmed appointment changes status to `PENDING`.
- A doctor or receptionist may preserve `CONFIRMED`; a pending appointment
  remains pending.
- A successful change records old and new intervals, old and new status, actor
  and timestamp.

## 7. Atomicity and concurrency

- Validation, conflict checks, appointment update and history insertion occur
  in one transaction.
- If any step fails, the original interval and status remain unchanged.
- Concurrent modifications cannot silently overwrite each other.
- Detailed locking and idempotency rules are defined by Spec 012.

## 8. HTTP behaviour

- Successful cancellation or rescheduling returns the role-appropriate updated
  appointment representation.
- Unknown or inaccessible appointment returns `404`.
- Deadline, status and concurrent state conflicts return `409`.
- Malformed or unsupported time values return `400`.

## 9. Acceptance scenarios

- A patient cancels more than two hours before the start.
- A patient cancellation inside two hours is rejected.
- Staff cancellation without a reason is rejected.
- Staff can cancel before, but not after, the start.
- Cancellation releases the slot and preserves history.
- A valid reschedule keeps the appointment ID.
- A patient reschedule returns confirmed to pending.
- A staff reschedule may preserve confirmed status.
- A conflicting reschedule fails without changing the original appointment.
