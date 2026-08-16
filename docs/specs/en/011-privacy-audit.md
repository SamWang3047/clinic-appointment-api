# 011 — Privacy, Sensitive Data and Audit

**Status:** Approved\
**Priority:** P1\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines application-level data minimisation, sensitive-field
visibility, safe logging and audit history. It is a project policy and does not
claim certification against a particular legal or medical standard.

## 2. Data classification

### Public

- Active doctor ID, name and specialty.
- Calculated available slot start/end and duration.

### Internal operational

- Account and profile IDs.
- Appointment start/end and status.
- Doctor schedules and availability configuration.
- Non-sensitive audit metadata.

### Sensitive

- Patient full name, email and phone.
- Appointment reason and cancellation reason.
- Association between a patient and an appointment.

### Secret

- Plaintext password and password hash.
- JWT and signing material.
- Database credentials.

## 3. Field visibility

| Data | Patient | Assigned doctor | Receptionist | Administrator | Public |
| --- | --- | --- | --- | --- | --- |
| Own appointment reason | Yes | Yes | Yes | No | No |
| Cancellation reason | Own | Assigned | Yes | No | No |
| Patient contact details | Own | Assigned need | Yes | Minimal/No | No |
| Appointment time/status | Own | Assigned | Yes | Yes | No |
| Free slot times | Yes | Yes | Yes | Yes | Yes |
| Password/hash/token | No | No | No | No | No |

“Assigned” means access is limited to the doctor's own appointment, not every
patient in the clinic.

## 4. API response design

- JPA entities are never serialised directly.
- Separate response DTOs or explicit views enforce role-specific fields.
- Public availability does not expose the reason a time is unavailable.
- Password hashes, optimistic-lock versions and internal audit records are not
  included in business responses.
- Collection responses apply the same field rules as detail responses.

## 5. Logging

Application and request logs must not contain:

- passwords or password hashes;
- complete JWTs or signing secrets;
- appointment or cancellation reasons;
- complete patient request or response bodies; or
- database credentials.

Logs may contain generated resource IDs, safe error codes, HTTP method, route,
status, duration and a correlation ID. Unexpected exceptions are logged
server-side while the client receives a generic message.

## 6. Audit events

Security- and appointment-relevant changes append an audit event for:

- account creation, disabling and role-relevant administrative changes;
- doctor creation and deactivation;
- availability creation or replacement;
- appointment creation, confirmation, decline, cancellation, rescheduling and
  completion; and
- rejected sensitive access when useful for security monitoring.

An appointment audit event records, where applicable:

```text
eventId
appointmentId
actorAccountId
actorRole
action
previousStatus
newStatus
previousStartAt
previousEndAt
newStartAt
newEndAt
occurredAt
correlationId
```

The audit event does not copy appointment reason, cancellation reason, password,
token or full patient details.

## 7. Audit integrity and access

- Audit events are append-only through the application API.
- Business operations cannot update or delete a previous event.
- Audit insertion participates in the same transaction as the successful
  business change when consistency is required.
- Failed transactions do not record a misleading success event.
- Administrators can inspect operational audit events; clinical reasons are not
  present in them.
- Audit list endpoints require pagination and explicit authorisation.

## 8. Error privacy

- Public and authentication errors do not reveal whether an email, patient or
  appointment exists beyond the agreed API semantics.
- Database constraint names, SQL, stack traces and internal class names are not
  returned to clients.
- Validation responses identify request fields but do not echo secret values.

## 9. Acceptance scenarios

- Public slot responses reveal no patient or appointment identity.
- A patient response contains only that patient's allowed sensitive data.
- An administrator appointment response omits reasons and unnecessary contact
  data.
- Logs contain no reason, password or complete JWT after success or failure.
- Every successful appointment state transition has one corresponding audit
  event.
- A rolled-back transition produces neither a state change nor a success audit.
- Audit responses contain actor/action metadata but no copied clinical reason.
