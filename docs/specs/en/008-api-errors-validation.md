# 008 — API Errors and Validation

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines a stable error contract, HTTP status semantics and
safe validation responses for every API endpoint.

## 2. Problem response

Errors use `application/problem+json` and a Problem Details representation:

```json
{
  "type": "urn:problem:clinic:appointment-conflict",
  "title": "Resource conflict",
  "status": 409,
  "detail": "The requested time is no longer available.",
  "instance": "/api/v1/me/appointments",
  "errorCode": "APPOINTMENT_CONFLICT",
  "correlationId": "01J00000000000000000000000",
  "violations": []
}
```

- `type`, `title`, `status`, `errorCode` and `correlationId` are always present.
- `detail` is safe for the caller and never contains an internal exception.
- `instance` identifies the request path without query secrets.
- `violations` is present for field validation and omitted otherwise.

## 3. HTTP status semantics

| Status | Meaning |
| --- | --- |
| `200` | Successful query or command returning a representation |
| `201` | Resource created; includes `Location` |
| `204` | Successful operation with no response body |
| `400` | Malformed JSON, invalid fields, range or business input |
| `401` | Missing, invalid or expired authentication |
| `403` | Authenticated role cannot invoke the capability |
| `404` | Resource absent or concealed by ownership rules |
| `409` | State, schedule, duplicate, optimistic lock or idempotency conflict |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error |

## 4. Stable error codes

The first release defines at least:

- `MALFORMED_REQUEST`
- `VALIDATION_FAILED`
- `BUSINESS_RULE_VIOLATION`
- `AUTHENTICATION_REQUIRED`
- `INVALID_CREDENTIALS`
- `ACCESS_DENIED`
- `RESOURCE_NOT_FOUND`
- `DUPLICATE_RESOURCE`
- `APPOINTMENT_CONFLICT`
- `INVALID_STATE_TRANSITION`
- `CONCURRENT_MODIFICATION`
- `IDEMPOTENCY_KEY_REUSED`
- `IDEMPOTENCY_REQUEST_IN_PROGRESS`
- `RATE_LIMITED`
- `INTERNAL_ERROR`

Error codes are part of the API contract and are not renamed within `/api/v1`
without a compatibility plan.

## 5. Field violations

Each validation violation contains:

```json
{
  "field": "durationMinutes",
  "message": "must be one of 15, 30, 45 or 60"
}
```

- Field names match the public JSON contract.
- Secret or complete rejected values are not echoed.
- Multiple independent violations may be returned together.
- Cross-field failures use the most relevant field or a safe object-level name.

## 6. Security and conflict handling

- Login failure is deliberately generic and does not reveal account existence.
- Ownership concealment returns the same `404` shape as an unknown resource.
- SQL, constraint names, stack traces and class names are never returned.
- Database uniqueness, lock and optimistic-version failures are translated into
  stable domain error codes.
- Unexpected exceptions are logged with correlation ID and returned as a
  generic `500`.

## 7. Correlation ID

- The API accepts a valid `X-Correlation-ID` or generates one.
- The response returns the correlation ID header.
- Problem responses also contain `correlationId`.
- A supplied value is length- and character-limited before logging.

## 8. Acceptance scenarios

- Invalid request fields return `400`, `VALIDATION_FAILED` and violations.
- Malformed JSON returns `400` without parser internals.
- A schedule collision returns `409 APPOINTMENT_CONFLICT`.
- Inaccessible and unknown appointments return indistinguishable `404` bodies.
- Unexpected failures return a safe `500` and a traceable correlation ID.
- Every documented endpoint references the shared problem schema.
