# 006 — View and Search Appointments

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines single-appointment reads, role-scoped lists,
filtering, pagination and field-level response privacy.

## 2. Single appointment access

- The owning patient may read the appointment.
- The assigned doctor may read the appointment.
- A receptionist may read clinic appointments.
- An administrator may read operational appointment data but not the reason.
- An authenticated user without resource access receives `404`, so the API does
  not reveal whether the appointment ID exists.

## 3. Role-scoped lists

### Patient

- Lists only appointments owned by the authenticated patient's profile.
- Cannot submit another patient ID as a filter.

### Doctor

- Lists only appointments assigned to the authenticated doctor's profile.
- Supports a selected Melbourne local date for a daily schedule.

### Receptionist

- Searches appointments across the single clinic.
- May filter by patient or doctor when coordinating care.

### Administrator

- Searches operational appointment schedules across the clinic.
- Does not receive appointment reasons by default.

Public users can query only calculated available slots from Spec 004, never an
appointment list.

## 4. Filters and ordering

Appointment list endpoints support:

- `from` UTC instant;
- `to` UTC instant;
- one or more appointment statuses; and
- patient or doctor ID only when the caller's role permits it.

Results are ordered by `startAt` ascending by default, with appointment ID as a
stable secondary order. Invalid ranges, including `to <= from`, return `400`.

## 5. Pagination

- Default page size is 20.
- Maximum page size is 100.
- Page numbers are zero-based.
- A response contains items, page, size, total elements and total pages.
- Requests above the maximum are rejected or capped consistently as defined by
  the OpenAPI contract; the first release uses rejection with `400`.

## 6. Response views

### Patient view

Includes appointment ID, safe doctor summary, start/end, status, reason and
timestamps. It does not include staff account data or audit internals.

### Doctor view

Includes appointment ID, necessary patient summary and contact data, start/end,
status and reason for the assigned doctor's own appointments.

### Receptionist view

Includes the fields needed to identify the patient, coordinate time and view
the reason within clinic duties.

### Administrator view

Includes IDs, safe doctor/patient names, start/end and status. It omits reason
and unnecessary patient contact data.

## 7. Query efficiency

- List queries must avoid N+1 loading of doctor and patient summaries.
- Pagination is applied in the database.
- APIs do not serialise JPA entities directly.

## 8. Acceptance scenarios

- A patient sees only their own appointments.
- A doctor sees only their assigned schedule.
- A receptionist can filter the clinic schedule by doctor and date.
- An administrator response omits the reason.
- An unrelated resource lookup returns `404`.
- Filters and pagination produce stable ordered results.
- A page size above 100 returns structured `400`.
