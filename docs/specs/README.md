# Clinic Appointment API Specifications

This directory is the source of truth for product behaviour and API contracts.
English specifications under `en/` are committed to Git. Chinese mirrors under
`zh-CN/` are maintained locally and excluded by `.gitignore`.

## Development workflow

For every feature:

1. Define or update the relevant product specification.
2. Agree on business rules and acceptance scenarios.
3. Update the OpenAPI contract when an HTTP interface changes.
4. Write failing acceptance or contract tests.
5. Implement the smallest change that satisfies the specification.
6. Run the full verification suite and record any deliberate deviation.

Implementation and tests must not silently redefine an approved specification.
If behaviour needs to change, the specification changes first.

The current implementation delta and recommended PR sequence are recorded in
[`en/IMPLEMENTATION-GAPS.md`](en/IMPLEMENTATION-GAPS.md).
The machine-readable HTTP contract is
[`openapi/clinic-api.yaml`](openapi/clinic-api.yaml).

## Specification catalogue

| ID | Specification | Priority | Status |
| --- | --- | --- | --- |
| 000 | Product scope and personas | P0 | Approved |
| 001 | Domain model and appointment lifecycle | P0 | Approved |
| 002 | Patient registration and profile | P0 | Approved |
| 003 | Doctor management | P0 | Approved |
| 004 | Doctor availability and slot calculation | P0 | Approved |
| 005 | Create and confirm appointment | P0 | Approved |
| 006 | View and search appointments | P0 | Approved |
| 007 | Cancel and reschedule appointment | P0 | Approved |
| 008 | API errors and validation | P0 | Approved |
| 009 | OpenAPI contract | P0 | Approved |
| 010 | Authentication and authorisation | P1 | Approved |
| 011 | Privacy, audit and sensitive data | P1 | Approved |
| 012 | Concurrency and idempotency | P1 | Approved |
| 013 | Acceptance test catalogue | P1 | Approved |
| 014 | Non-functional requirements | P1 | Approved |
| 015 | Notifications and reminders | P2 | Deferred |
| 016 | Deployment, operations and observability | P2 | Approved |

## Status definitions

- **Draft**: questions remain and the behaviour is not ready to implement.
- **Approved**: business decisions and acceptance scenarios are agreed.
- **Implemented**: implementation and automated tests satisfy the approved spec.
- **Deferred**: intentionally outside the current delivery scope.
