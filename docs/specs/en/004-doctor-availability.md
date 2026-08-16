# 004 — Doctor Availability and Slot Calculation

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines how doctors configure working time and how the API
calculates bookable appointment starts.

## 2. Time model

- A doctor configures working intervals using `Australia/Melbourne` local day
  and time values.
- API appointment timestamps are UTC ISO 8601 instants.
- Time-zone conversion uses the `Australia/Melbourne` zone rules, including
  daylight-saving changes, rather than a fixed UTC offset.
- A local date is interpreted according to Melbourne time.

## 3. Recurring weekly availability

- A doctor may have zero or more non-overlapping intervals on each day of the
  week.
- Multiple intervals represent breaks, for example 09:00–12:00 and
  13:00–17:00.
- End time must be after start time.
- Adjacent intervals may be accepted but should be normalised or returned in a
  consistent order.
- A day without an interval is not a working day.

Doctors may update their own recurring hours. Receptionists and administrators
may update them on a doctor's behalf.

## 4. Date-specific overrides

- An override applies to one doctor and one Melbourne local date.
- When an override exists, it replaces the recurring intervals for that date.
- An override with no intervals represents leave, a public holiday or another
  fully unavailable day.
- An override with intervals can shorten, extend or otherwise replace the
  normal working day.
- Multiple overlapping overrides for the same doctor and date are not allowed.

## 5. Slot request

The first release calculates slots for:

- one active doctor;
- one Melbourne local date; and
- one requested duration from 15, 30, 45 or 60 minutes.

The response contains UTC start and end instants. It does not contain patient
details, appointment reasons or the identity of whoever occupies an unavailable
time.

## 6. Slot calculation

For each effective working interval:

1. Start at the first 15-minute boundary in the interval.
2. Create candidate starts every 15 minutes.
3. Calculate the candidate end from the requested duration.
4. Keep the candidate only when the complete interval fits within the same
   working interval.
5. Remove past candidates.
6. Remove candidates overlapping a `PENDING` or `CONFIRMED` appointment.

There is no buffer between appointments in the first release. Adjacent
appointments are valid.

## 7. Validation and errors

- An unknown doctor returns `404 Resource Not Found`.
- An inactive doctor has no public bookable slots.
- An unsupported duration returns `400 Business Rule Violation`.
- An invalid date value returns `400 Malformed Request`.
- A valid date with no availability returns an empty collection, not `404`.

## 8. Privacy

Public availability exposes only free start/end instants and the requested
duration. It must not reveal why another period is unavailable or whether it is
occupied by an appointment, leave or a manual override.

## 9. Acceptance scenarios

### Recurring working day

Given a doctor works Monday 09:00–12:00\
And there are no active appointments\
When 30-minute slots are requested for that Monday\
Then starts are returned every 15 minutes from 09:00 through 11:30.

### Break between intervals

Given a doctor works 09:00–12:00 and 13:00–17:00\
When 60-minute slots are requested\
Then no slot crosses the 12:00–13:00 break.

### Date override

Given recurring hours exist for a Tuesday\
And an empty override exists for that date\
When slots are requested\
Then the response is empty.

### Active appointment conflict

Given a confirmed appointment occupies 10:00–10:30\
When 30-minute slots are requested\
Then every candidate overlapping that interval is excluded.

### Terminal appointment

Given a cancelled or declined appointment previously occupied 10:00–10:30\
When slots are requested\
Then that appointment does not block candidates.

### Inactive doctor

Given a doctor is inactive\
When public slots are requested\
Then the response contains no bookable slots.
