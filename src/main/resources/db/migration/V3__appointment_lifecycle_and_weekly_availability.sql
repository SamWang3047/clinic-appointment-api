ALTER TABLE appointments DROP CONSTRAINT chk_appointments_status;

UPDATE appointments
SET status = 'CONFIRMED'
WHERE status = 'BOOKED';

ALTER TABLE appointments
    ADD CONSTRAINT chk_appointments_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    ADD COLUMN cancellation_reason VARCHAR(500),
    ADD CONSTRAINT chk_appointments_cancellation_reason_not_blank
        CHECK (cancellation_reason IS NULL OR length(trim(cancellation_reason)) > 0);

CREATE TABLE doctor_weekly_intervals (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doctor_weekly_intervals_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT chk_doctor_weekly_intervals_day
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_doctor_weekly_intervals_range CHECK (end_time > start_time),
    CONSTRAINT uk_doctor_weekly_intervals
        UNIQUE (doctor_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_doctor_weekly_intervals_lookup
    ON doctor_weekly_intervals (doctor_id, day_of_week, start_time);

CREATE TABLE appointment_audit_events (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    actor_account_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    previous_start_at TIMESTAMPTZ,
    previous_end_at TIMESTAMPTZ,
    new_start_at TIMESTAMPTZ,
    new_end_at TIMESTAMPTZ,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    CONSTRAINT fk_appointment_audit_events_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_audit_events_actor
        FOREIGN KEY (actor_account_id) REFERENCES user_accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_appointment_audit_events_actor_role
        CHECK (actor_role IN ('PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN')),
    CONSTRAINT chk_appointment_audit_events_action
        CHECK (action IN ('CREATED', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'RESCHEDULED', 'COMPLETED')),
    CONSTRAINT chk_appointment_audit_events_previous_status
        CHECK (previous_status IS NULL OR previous_status IN ('PENDING', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_appointment_audit_events_new_status
        CHECK (new_status IS NULL OR new_status IN ('PENDING', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_appointment_audit_events_correlation_id_not_blank
        CHECK (length(trim(correlation_id)) > 0)
);

CREATE INDEX idx_appointment_audit_events_appointment_time
    ON appointment_audit_events (appointment_id, occurred_at, id);
