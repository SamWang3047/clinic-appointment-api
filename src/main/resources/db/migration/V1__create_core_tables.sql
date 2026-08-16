CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_doctors_full_name_not_blank CHECK (length(trim(full_name)) > 0),
    CONSTRAINT chk_doctors_specialty_not_blank CHECK (length(trim(specialty)) > 0)
);

CREATE TABLE patients (
    id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_patients_email UNIQUE (email),
    CONSTRAINT chk_patients_full_name_not_blank CHECK (length(trim(full_name)) > 0),
    CONSTRAINT chk_patients_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT chk_patients_phone_not_blank CHECK (length(trim(phone)) > 0)
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE RESTRICT,
    CONSTRAINT chk_appointments_time_range CHECK (end_at > start_at),
    CONSTRAINT chk_appointments_status
        CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_appointments_reason_not_blank CHECK (length(trim(reason)) > 0)
);

CREATE INDEX idx_appointments_doctor_start
    ON appointments (doctor_id, start_at);

CREATE INDEX idx_appointments_patient_start
    ON appointments (patient_id, start_at);

CREATE INDEX idx_appointments_status
    ON appointments (status);
