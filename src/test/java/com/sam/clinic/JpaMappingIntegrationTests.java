package com.sam.clinic;

import static org.assertj.core.api.Assertions.assertThat;

import com.sam.clinic.appointment.Appointment;
import com.sam.clinic.appointment.AppointmentRepository;
import com.sam.clinic.appointment.AppointmentStatus;
import com.sam.clinic.doctor.Doctor;
import com.sam.clinic.doctor.DoctorRepository;
import com.sam.clinic.patient.Patient;
import com.sam.clinic.patient.PatientRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JpaMappingIntegrationTests {

	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final AppointmentRepository appointmentRepository;
	private final EntityManager entityManager;

	@Autowired
	JpaMappingIntegrationTests(
			DoctorRepository doctorRepository,
			PatientRepository patientRepository,
			AppointmentRepository appointmentRepository,
			EntityManager entityManager) {
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.appointmentRepository = appointmentRepository;
		this.entityManager = entityManager;
	}

	@Test
	void persistsAndReloadsTheCoreDomainModel() {
		Doctor doctor = doctorRepository.save(new Doctor("Dr Alice Chen", "General Practice"));
		Patient patient = patientRepository.save(
				new Patient("Sam Patient", "SAM.PATIENT@example.com", "+61 400 000 000"));
		Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
		Instant endAt = startAt.plus(30, ChronoUnit.MINUTES);
		Appointment appointment = appointmentRepository.saveAndFlush(
				new Appointment(doctor, patient, startAt, endAt, "Initial consultation"));
		UUID appointmentId = appointment.getId();

		entityManager.clear();

		Appointment reloaded = appointmentRepository.findById(appointmentId).orElseThrow();

		assertThat(reloaded.getDoctor().getFullName()).isEqualTo("Dr Alice Chen");
		assertThat(reloaded.getPatient().getEmail()).isEqualTo("sam.patient@example.com");
		assertThat(reloaded.getStartAt()).isEqualTo(startAt);
		assertThat(reloaded.getEndAt()).isEqualTo(endAt);
		assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
		assertThat(reloaded.getReason()).isEqualTo("Initial consultation");
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getUpdatedAt()).isNotNull();
	}
}
