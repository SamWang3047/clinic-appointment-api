package com.sam.clinic.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = {})
@NotBlank
@Size(min = 10, max = 128)
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$")
@ReportAsSingleViolation
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

	String message() default "must be 10 to 128 characters and contain at least one letter and one digit";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
