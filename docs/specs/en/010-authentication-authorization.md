# 010 — Authentication and Authorisation

**Status:** Approved\
**Priority:** P1\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines accounts, login, JWT authentication, role checks and
resource ownership. Authentication proves identity; authorisation decides what
that identity may do.

## 2. Account model

`UserAccount` contains:

- system-generated UUID;
- normalised unique login email;
- BCrypt password hash;
- exactly one role: `PATIENT`, `DOCTOR`, `RECEPTIONIST` or `ADMIN`;
- active or disabled status; and
- optional one-to-one patient or doctor profile link required by those roles.

Passwords and JWTs are never stored on patient, doctor or appointment entities.

## 3. Account creation

- Public patient registration creates a `PATIENT` account and profile.
- A receptionist can create a patient profile without credentials.
- Only an administrator creates doctor, receptionist or administrator accounts.
- A doctor account must link to exactly one doctor profile.
- Role is assigned by the server-side use case, not trusted from public request
  JSON.
- Profile claiming, password reset and password change are deferred.

## 4. Password policy

- Password is at least 10 characters.
- It contains at least one letter and one digit.
- It is hashed with BCrypt before transaction commit.
- Plaintext password and hash never appear in API responses, logs or audit
  events.
- Login uses one generic failure response for unknown email, wrong password and
  disabled account.

## 5. Login and JWT

- Login accepts email and password over HTTPS in deployed environments.
- Success returns a signed Bearer access token valid for 30 minutes.
- The first release has no refresh token.
- The token subject is account UUID and includes the single role; sensitive
  profile data is excluded.
- JWT signing material comes from environment configuration and is never
  committed.
- Every protected request uses `Authorization: Bearer <token>`.
- A disabled account is rejected even if presented with an otherwise valid
  token.
- Logout is client-side token disposal in the first release.

## 6. Public endpoints

Only these capabilities are anonymous:

- login;
- patient self-registration;
- list active doctors;
- retrieve an active doctor's public detail; and
- retrieve an active doctor's public available slots.

All patient profiles, appointments, schedules, availability management and
administration operations require authentication.

## 7. Role permissions

### Patient

- Reads their own profile and appointments.
- Creates, cancels and reschedules only their own appointments.
- Cannot provide another patient ID for a self-service operation.

### Doctor

- Reads their own profile and assigned appointments.
- Manages their own availability.
- Confirms, declines, completes, cancels or reschedules only assigned
  appointments when the domain transition permits it.

### Receptionist

- Creates patient profiles and books on behalf of patients.
- Reads clinic schedules and patient contact details needed for coordination.
- Confirms, declines, cancels and reschedules according to domain rules.
- Manages doctor availability on a doctor's behalf.

### Administrator

- Creates and disables staff accounts and manages doctor profiles.
- Reads operational clinic schedules without appointment reasons by default.
- Manages doctor availability and may cancel an appointment under clinic rules.
- Does not automatically receive clinical-detail access.

## 8. Ownership enforcement

- Role checks alone are insufficient for patient and doctor resources.
- Patient ownership is derived from the authenticated account/profile link.
- Doctor assignment is derived from the authenticated account/profile link.
- IDs supplied in a path or request never override authenticated ownership.
- Service-layer authorisation protects use cases even if invoked outside a
  controller.

## 9. Security error behaviour

- Missing, malformed, expired or invalid credentials return `401`.
- A valid identity calling a role-restricted capability returns `403`.
- A valid identity requesting a specific patient or appointment they cannot
  access receives `404` to conceal resource existence.
- Authentication errors do not reveal whether an email is registered.

## 10. Technical security boundary

- The API is stateless and does not create HTTP sessions.
- CSRF protection is not required for Bearer-token API requests because tokens
  are not automatically sent by the browser as cookies.
- CORS is denied by default until an explicit client origin is configured.
- Method and service-level rules must be covered by automated security tests.

## 11. Acceptance scenarios

- Valid credentials return a 30-minute JWT without password data.
- Invalid email and invalid password produce indistinguishable responses.
- A patient cannot read or mutate another patient's appointment.
- An unrelated doctor cannot read or confirm another doctor's appointment.
- A receptionist can book for a selected existing patient.
- An administrator can create a doctor account but does not receive appointment
  reasons.
- A disabled account cannot use a previously issued token.
- Public availability remains accessible without authentication and exposes no
  appointment identity.
