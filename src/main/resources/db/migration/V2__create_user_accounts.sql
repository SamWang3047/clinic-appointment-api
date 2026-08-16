CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    login_email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    patient_id UUID,
    doctor_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_accounts_login_email UNIQUE (login_email),
    CONSTRAINT uk_user_accounts_patient UNIQUE (patient_id),
    CONSTRAINT uk_user_accounts_doctor UNIQUE (doctor_id),
    CONSTRAINT fk_user_accounts_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_accounts_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_accounts_login_email_not_blank
        CHECK (length(trim(login_email)) > 0),
    CONSTRAINT chk_user_accounts_password_hash_not_blank
        CHECK (length(trim(password_hash)) > 0),
    CONSTRAINT chk_user_accounts_role
        CHECK (role IN ('PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN')),
    CONSTRAINT chk_user_accounts_profile_matches_role CHECK (
        (role = 'PATIENT' AND patient_id IS NOT NULL AND doctor_id IS NULL)
        OR (role = 'DOCTOR' AND doctor_id IS NOT NULL AND patient_id IS NULL)
        OR (role IN ('RECEPTIONIST', 'ADMIN') AND patient_id IS NULL AND doctor_id IS NULL)
    )
);
