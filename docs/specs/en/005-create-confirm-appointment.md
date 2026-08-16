# 005 — Create, Confirm and Decline Appointment

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines appointment requests and the decision that changes a
request from `PENDING` to `CONFIRMED` or `DECLINED`.

## 2. Patient creation flow

- A patient creates an appointment through an authenticated self-service
  endpoint such as `POST /api/v1/me/appointments`.
- Patient identity is derived from the authenticated account.
- The request must not accept a `patientId`; a patient cannot book for another
  patient by changing JSON.
- The request contains doctor ID, UTC start instant, duration minutes and
  reason. The server derives the end instant.

## 3. Receptionist creation flow

- A receptionist creates an appointment through the staff appointment
  collection, such as `POST /api/v1/appointments`.
- The request explicitly identifies an existing patient and doctor.
- Doctor and patient IDs are validated before availability checks.
- Doctors and administrators do not use this endpoint to impersonate a patient.

## 4. Creation rules

- All invariants in Specs 001 and 004 apply.
- The doctor must be active and the complete interval must be available.
- Duration is one of 15, 30, 45 or 60 minutes.
- The start is a UTC instant corresponding to a valid 15-minute slot.
- Reason is required and limited to 500 characters.
- `PENDING` and `CONFIRMED` appointments block both doctor and patient time.
- A successful request creates status `PENDING` and returns `201 Created` with
  a `Location` header.
- A schedule conflict returns `409` and creates no appointment.
- Creation is transactional and concurrency-safe according to Spec 012.

## 5. Confirmation

- Only the assigned doctor or a receptionist may confirm.
- Only `PENDING` can transition to `CONFIRMED`.
- Confirmation is rejected after the appointment has started.
- Confirmation records actor and timestamp in audit history.
- Repeated confirmation must not create another appointment or notification.

## 6. Decline

- Only the assigned doctor or a receptionist may decline.
- Only `PENDING` can transition to `DECLINED`.
- Decline is rejected after the appointment has started.
- Decline releases the doctor and patient time immediately.
- Decline records actor and timestamp in audit history.

## 7. Response privacy

- The owning patient, assigned doctor and receptionist may receive the reason.
- An administrator response omits the reason.
- Public APIs never expose the appointment response.
- Responses never expose password data, internal JPA version fields or audit
  internals.

## 8. Acceptance scenarios

- A patient books for themself without supplying a patient ID.
- A receptionist books for an existing patient by ID.
- A valid request creates exactly one pending appointment.
- A request outside availability is rejected.
- Doctor or patient overlap returns `409`.
- The assigned doctor or receptionist confirms a pending appointment.
- An unrelated doctor cannot confirm it.
- Confirmation or decline of a terminal appointment fails with `409`.
- Declining releases the slot without deleting appointment history.
