# 015 — Notifications and Reminders

**Status:** Deferred\
**Priority:** P2\
**Last updated:** 2026-08-16

## Decision

The first release does not send real email or SMS messages. Appointment
creation, confirmation, decline, cancellation and rescheduling may expose
internal domain events later, but external providers, delivery retries,
templates and patient communication preferences are outside the interview MVP.

Business transactions must not call an external notification provider directly.
A future implementation should use an after-commit or transactional-outbox
boundary so notification failure cannot roll back an already valid appointment.
