# Five-minute interview demo

## Story

This demo follows one appointment from discovery to cancellation:

1. A patient discovers a public 30-minute slot and creates a pending request.
2. The assigned doctor confirms it.
3. An administrator can see operational data, but not the clinical reason or
   patient contact details.
4. The patient cancels more than two hours before the start.
5. The cancelled interval immediately appears in public availability again.

Run `demo/interview-demo.ps1` after starting the application with the `demo`
profile. The script prints only IDs, status values and privacy assertions. It
does not print JWTs or clinical reasons.

## What to explain while the script runs

### 1. API and identity boundary

“The patient uses `/api/v1/me/appointments`, so patient identity comes from the
JWT instead of a request-supplied patient ID. A receptionist has a separate
staff endpoint for booking on behalf of an existing patient.”

### 2. Transaction and conflict protection

“The service validates the slot inside one transaction. It locks the doctor,
then the patient, checks weekly availability, and performs half-open interval
overlap queries against pending and confirmed appointments. The fixed lock
order serialises competing bookings without raising isolation globally.”

### 3. State machine

“The entity exposes explicit commands such as `confirm` and `cancel`; there is
no generic status setter. This keeps valid transitions and time rules close to
the domain model. `@Version` prevents concurrent state changes from silently
overwriting each other.”

### 4. Privacy and audit

“JPA entities never leave the service. The response mapper includes the reason
and patient contact only for the owner, assigned doctor or receptionist. Admin
responses are deliberately minimised. Every successful mutation appends a
reason-free audit event in the same transaction using the request correlation
ID.”

### 5. Honest scope boundary

“This interview slice implements the highest-value vertical path. Date-specific
availability overrides, durable idempotency replay, rescheduling, notifications
and the audit administration endpoint remain explicit follow-up work in the
approved target specification.”

## Likely follow-up questions

**Why pessimistic locks for creation and optimistic locking for status?**

Creation must prevent two independently new rows from passing the same overlap
check, so it serialises on the existing doctor and patient rows. Status changes
operate on one existing aggregate, where an optimistic version detects stale
writes efficiently.

**Why are intervals half-open?**

Using `[start, end)` means a 10:00–10:30 appointment can be followed by a
10:30–11:00 appointment without being treated as an overlap.

**Why store weekly hours without a time zone?**

“Monday 09:00” is Melbourne wall-clock time, not an instant. It is converted
through `Australia/Melbourne` zone rules only when generating a concrete date,
which handles daylight-saving offsets correctly.

**How would you add idempotency?**

Persist a unique `(accountId, operation, idempotencyKey)` record with a request
hash and stored final response. Reserve it before executing, replay identical
completed requests, reject a changed payload, and keep the audit write inside
the original business transaction.
