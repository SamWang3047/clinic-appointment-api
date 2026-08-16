# 009 — HTTP and OpenAPI Contract

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Source of truth

`docs/specs/openapi/clinic-api.yaml` is the machine-readable source of truth for
HTTP paths, methods, security requirements, parameters and schemas. Controller
behaviour must conform to it.

The delivery order for an API change is:

1. update the business specification;
2. update OpenAPI;
3. add or update contract and acceptance tests;
4. implement the controller, DTO and service changes; and
5. verify compatibility and the full build.

## 2. General conventions

- Base path is `/api/v1`.
- JSON uses UTF-8 and `application/json`.
- Errors use `application/problem+json`.
- IDs are UUID strings.
- Instants are ISO 8601 UTC values ending in `Z`.
- Melbourne calendar dates use `YYYY-MM-DD`.
- Local working times use `HH:mm` and are interpreted in
  `Australia/Melbourne`.
- Property names use lower camel case.

## 3. Authentication

- Protected operations use an HTTP Bearer JWT security scheme.
- Public operations explicitly declare no security.
- Security requirements in OpenAPI describe authentication; service rules still
  enforce roles and resource ownership.

## 4. Resource and command style

Queries and creation use resource collections. Domain state transitions use
explicit command endpoints:

```text
POST /api/v1/appointments/{appointmentId}/confirm
POST /api/v1/appointments/{appointmentId}/decline
POST /api/v1/appointments/{appointmentId}/cancel
POST /api/v1/appointments/{appointmentId}/reschedule
POST /api/v1/appointments/{appointmentId}/complete
```

Each successful command returns the role-appropriate current appointment
representation. Command names make transition-specific validation visible and
avoid a generic status setter.

## 5. Self-service and staff boundaries

- Patient self-service appointments use `/api/v1/me/appointments` and derive
  patient identity from authentication.
- Staff appointment creation uses `/api/v1/appointments` and accepts patient ID.
- `/me` requests never accept another profile ID.
- Public availability is separate from internal doctor schedules.

## 6. Pagination and filtering

- Pageable collections use zero-based `page` and `size` parameters.
- Default size is 20 and maximum is 100.
- Appointment lists support `from`, `to` and `status` filters.
- Responses include `items`, `page`, `size`, `totalElements` and `totalPages`.

## 7. Idempotency

Appointment creation and state-changing commands require `Idempotency-Key`.
Its behaviour is defined by Spec 012 and documented on each applicable OpenAPI
operation.

## 8. Compatibility

- Adding an optional response field is normally compatible.
- Removing or renaming a field, changing its type, making optional input
  required or changing status semantics is breaking.
- Breaking changes require a new API version or an agreed migration period.
- Existing `/api/v1` behaviour is not silently repurposed.

## 9. Contract verification

- OpenAPI syntax is validated in CI.
- MVC tests validate representative success and Problem Details responses.
- Security tests validate public/protected operation boundaries.
- The checked-in contract and generated/runtime API description, if enabled,
  must not drift silently.
