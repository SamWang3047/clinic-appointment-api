# 000 — Product Scope and Personas

**Status:** Approved\
**Priority:** P0\
**Last updated:** 2026-08-16

## 1. Purpose

The Clinic Appointment API supports appointment coordination between patients
and clinic staff. Both patients and receptionists can create appointments.
Doctors manage their regular availability and participate in appointment
confirmation and completion. Administrators manage the system and its users.

The first release is an interview demonstration. It prioritises clear domain
modelling, secure API boundaries, explainable Spring design, automated tests and
reliable concurrency behaviour over a broad production feature set.

## 2. Personas

### Patient

- Registers and maintains a patient identity.
- Searches for doctors and available appointment times.
- Creates an appointment for themself.
- Views, cancels or reschedules their own appointments.
- Can view the appointment reason they supplied.

### Receptionist

- Creates appointments on behalf of patients.
- Views clinic schedules needed to coordinate appointments.
- Confirms, cancels and reschedules appointments within clinic policy.
- Can view appointment reasons when required to coordinate care.

### Doctor

- Configures recurring weekly working hours.
- Views their own schedule and appointment details.
- Confirms pending appointments.
- Completes appointments.
- Can view appointment reasons for their own appointments.

### Administrator

- Manages doctors, staff and system configuration.
- Has operational access needed to support the clinic.
- Does not automatically receive unrestricted access to clinical information;
  sensitive access must be justified by an explicit authorisation rule.

## 3. MVP scope

### Doctor and availability management

- Create, view and deactivate doctors.
- Configure recurring weekly working hours for a doctor.
- Calculate available appointment times from working hours and existing
  appointments.
- Store API timestamps in UTC and display them to users in the
  `Australia/Melbourne` time zone.

### Patient management

- Register a patient using a unique email address.
- Use an internal UUID as the persistent patient identifier.
- Protect patient contact details from unrelated users.

### Appointment management

- Allow a patient or receptionist to create an appointment.
- Allow durations of 15, 30, 45 or 60 minutes.
- Create appointments in the `PENDING` state and require a doctor or
  receptionist to confirm them.
- View one appointment.
- List appointments belonging to a patient.
- View a doctor's schedule for a selected day.
- Cancel an appointment.
- Reschedule an appointment.
- Return available times for a doctor.
- Prevent overlapping active appointments for both doctors and patients.

### Appointment reason

- A reason is required when an appointment is created.
- The reason is sensitive information.
- It is visible only to the patient who owns the appointment, the assigned
  doctor, and authorised reception staff.
- It must not appear in public doctor availability responses or unrelated
  users' responses and logs.

## 4. Delivery priorities

1. Correct and explainable business behaviour.
2. Explicit API and privacy contracts.
3. Automated unit, integration and acceptance tests.
4. Safe transactional and concurrent appointment creation.
5. A concise demonstration path suitable for a technical interview.

## 5. Out of scope for the first release

- Payments, Medicare and private insurance integration.
- Electronic medical records and clinical notes.
- Prescriptions.
- Video consultations.
- File uploads.
- Multi-clinic or multi-tenant support.
- Sending real SMS or email messages.
- Patient-facing web or mobile user interfaces.

Notification events and reminder interfaces may be designed, but external
delivery is deferred. The first release represents one clinic. A registered
patient may book without email verification.

## 6. Constraints

- The backend uses Java 21, Spring Boot, Spring Data JPA and PostgreSQL.
- Database schema changes are versioned using Flyway.
- API timestamps use ISO 8601 UTC instants.
- The English specifications and OpenAPI contract are committed to Git.
- Chinese specification mirrors are retained locally for study and review.

## 7. Confirmed product decisions

- Appointment states are `PENDING`, `CONFIRMED`, `DECLINED`, `CANCELLED` and
  `COMPLETED`. `BOOKED` and `NO_SHOW` are not used in the first release.
- `PENDING` and `CONFIRMED` appointments both reserve time.
- Pending appointments remain pending until a doctor or receptionist confirms
  or declines them; they do not expire automatically.
- Doctors manage their own availability. Receptionists and administrators may
  update availability on a doctor's behalf.
- Availability supports recurring weekly intervals, breaks and date-specific
  overrides.
- Appointment durations are 15, 30, 45 or 60 minutes and start on a 15-minute
  grid. There is no buffer between appointments in the first release.
- Patients may cancel until two hours before the start. Authorised staff may
  cancel until the appointment starts. Staff must provide a cancellation
  reason; a patient cancellation reason is optional.
- Rescheduling keeps the appointment ID and is recorded in audit history. A
  patient rescheduling a confirmed appointment returns it to `PENDING`;
  authorised staff may preserve `CONFIRMED`.
- Doctors can see their own full schedule. Receptionists and administrators can
  access clinic schedules. Patients can access only their own appointments and
  public available slots.
- The first release covers one clinic, does not require email verification and
  retains the exclusions listed above.

## 8. Definition of done

- All P0 specifications are approved.
- The OpenAPI document agrees with controller behaviour.
- Each business rule has at least one acceptance scenario.
- Role and ownership checks protect patient and appointment data.
- Concurrent requests cannot create overlapping active appointments.
- The Maven verification build passes against PostgreSQL.
- The README contains a repeatable interview demonstration.
