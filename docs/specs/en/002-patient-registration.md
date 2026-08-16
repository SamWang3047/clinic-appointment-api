# 002 — Patient Registration and Profile

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines patient self-registration, receptionist-created
patient profiles, identity uniqueness and profile privacy.

## 2. Account and profile separation

- `UserAccount` owns login email, password hash, role and account status.
- `Patient` owns the patient's name, contact email and phone number.
- A self-registered patient account is linked one-to-one to a patient profile.
- A receptionist may create a patient profile without creating login
  credentials.
- Claiming a receptionist-created profile is deferred from the first release.

## 3. Patient self-registration

The public registration request contains:

- full name;
- email;
- phone number; and
- password.

Registration atomically creates one active `PATIENT` account and one linked
patient profile. A partial account or profile must not remain after a failure.

The email is trimmed and lower-cased using locale-independent rules before
uniqueness checks and persistence. It must be unique across login accounts and
patient profiles. A conflict returns `409` without identifying the existing
record.

## 4. Receptionist-created profile

- An authenticated receptionist may create a patient profile for appointment
  coordination.
- The request does not accept a password and does not create a login account.
- The email follows the same normalisation and uniqueness rules.
- The response clearly indicates that the profile has no linked login account.
- Profile claiming and invitations are deferred.

## 5. Validation

- Full name is required and limited to 100 characters.
- Email is required, syntactically valid and limited to 255 characters.
- Phone is required and limited to 32 characters.
- A self-registration password is at least 10 characters and contains at least
  one letter and one digit.
- Leading and trailing whitespace is removed from text fields.

## 6. Access and response rules

- A patient may read their own profile.
- Receptionists may search and read patient contact details for this clinic.
- An assigned doctor may read only the patient information required for their
  own appointments.
- Administrators manage accounts but do not automatically receive clinical
  appointment reasons.
- Public registration responses never contain a password, password hash or
  authentication token other than through the login contract.
- Access to a specific unauthorised patient resource is reported as `404`.

## 7. Acceptance scenarios

- A valid self-registration creates a linked patient account and profile.
- Email matching is case-insensitive after normalisation.
- A duplicate email returns `409` and creates no new records.
- Invalid input returns structured field violations.
- A receptionist can create a profile without a password or login account.
- An unrelated patient cannot read another patient's profile.
- No patient response serialises password data.
