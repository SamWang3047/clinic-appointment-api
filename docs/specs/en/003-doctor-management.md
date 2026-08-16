# 003 — Doctor Management

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

This specification defines doctor creation, public discovery, account linkage
and deactivation. Availability behaviour is defined by Spec 004.

## 2. Doctor profile

A doctor profile contains:

- system-generated UUID;
- full name;
- specialty; and
- active flag.

The profile does not contain password or token fields. A doctor login uses a
separate `UserAccount` linked one-to-one to the profile.

## 3. Creation and staff account linkage

- Only an administrator may create a doctor profile and doctor login account.
- The account role is `DOCTOR` and cannot be selected by the request.
- Profile and account creation are atomic.
- The initial password follows Spec 010 and is stored only as a BCrypt hash.
- The response never returns the submitted password or hash.
- Creating receptionist and administrator accounts is also restricted to an
  administrator, but those accounts do not require doctor profiles.

## 4. Public discovery

- Anyone may list active doctors and retrieve an active doctor's public detail.
- Public responses contain only ID, full name, specialty and active status.
- Inactive doctors are omitted from the public list.
- A direct public lookup of an inactive or unknown doctor returns `404`.
- Public discovery never exposes login email, schedule occupancy or patient
  information.

## 5. Internal access

- A doctor may read their own profile and manage their own availability.
- Receptionists and administrators may read all doctor profiles needed for
  clinic operations.
- Administrators manage doctor identity fields and account status.
- Receptionists and administrators may manage availability on a doctor's
  behalf, as defined in Spec 004.

## 6. Deactivation

- Only an administrator may deactivate a doctor profile.
- Deactivation prevents new appointments and removes public availability.
- Historical appointments retain their doctor relationship.
- Deactivation does not delete the doctor.
- Existing future appointments are not silently cancelled; the API reports
  them so staff can resolve them explicitly.
- Login account disabling is an explicit administrator action and is audited.

## 7. Validation

- Full name is required and limited to 100 characters.
- Specialty is required and limited to 80 characters.
- Text values are trimmed before persistence.

## 8. Acceptance scenarios

- An administrator can atomically create a doctor profile and account.
- A non-administrator cannot create or deactivate a doctor.
- Public listing returns only active doctors and safe fields.
- An inactive doctor cannot receive a new appointment.
- Deactivation preserves historical appointments.
- A doctor can change their own availability but not another doctor's.
