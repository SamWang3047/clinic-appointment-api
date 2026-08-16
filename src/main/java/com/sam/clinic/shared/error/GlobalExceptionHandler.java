package com.sam.clinic.shared.error;

import com.sam.clinic.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final ProblemDetailsFactory problemDetailsFactory;

	public GlobalExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
		this.problemDetailsFactory = problemDetailsFactory;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
		return response(exception.getErrorCode(), exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidation(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<Violation> violations = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new Violation(
						error.getField(),
						Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
				.sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
				.toList();
		return validationResponse(violations, request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ProblemDetail> handleMethodValidation(
			HandlerMethodValidationException exception, HttpServletRequest request) {
		List<Violation> parameterViolations = exception.getParameterValidationResults().stream()
				.flatMap(result -> {
					String parameterName = Objects.requireNonNullElse(
							result.getMethodParameter().getParameterName(),
							"parameter" + result.getMethodParameter().getParameterIndex());
					return result.getResolvableErrors().stream()
							.map(error -> new Violation(
									parameterName,
									Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")));
				})
				.toList();
		List<Violation> crossParameterViolations = exception.getCrossParameterValidationResults().stream()
				.map(error -> new Violation(
						"request",
						Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
				.toList();
		List<Violation> violations = Stream.concat(
				parameterViolations.stream(), crossParameterViolations.stream())
				.sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
				.toList();
		return validationResponse(violations, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException exception, HttpServletRequest request) {
		List<Violation> violations = exception.getConstraintViolations().stream()
				.map(violation -> new Violation(
						lastPathSegment(violation.getPropertyPath()),
						violation.getMessage()))
				.sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
				.toList();
		return validationResponse(violations, request);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception, HttpServletRequest request) {
		return response(
				ApiErrorCode.MALFORMED_REQUEST,
				"The request contains malformed JSON or an invalid value.",
				request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> handleUnknownRoute(
			NoResourceFoundException exception, HttpServletRequest request) {
		return response(ApiErrorCode.RESOURCE_NOT_FOUND, "The requested resource was not found.", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataConflict(
			DataIntegrityViolationException exception, HttpServletRequest request) {
		return response(
				ApiErrorCode.DUPLICATE_RESOURCE,
				"The request conflicts with an existing resource.",
				request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> handleConcurrentModification(
			OptimisticLockingFailureException exception, HttpServletRequest request) {
		return response(
				ApiErrorCode.CONCURRENT_MODIFICATION,
				"The resource changed while the request was being processed.",
				request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(
			AccessDeniedException exception, HttpServletRequest request) {
		return response(
				ApiErrorCode.ACCESS_DENIED,
				"The authenticated account cannot perform this operation.",
				request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
		LOGGER.error(
				"Unhandled API error; correlationId={}; exceptionType={}",
				CorrelationIdFilter.from(request),
				exception.getClass().getName());
		return response(ApiErrorCode.INTERNAL_ERROR, "An unexpected error occurred.", request);
	}

	private ResponseEntity<ProblemDetail> validationResponse(
			List<Violation> violations, HttpServletRequest request) {
		ProblemDetail problem = problemDetailsFactory.create(
				ApiErrorCode.VALIDATION_FAILED,
				"One or more request fields are invalid.",
				request);
		problem.setProperty("violations", violations);
		return response(problem);
	}

	private ResponseEntity<ProblemDetail> response(
			ApiErrorCode errorCode, String detail, HttpServletRequest request) {
		return response(problemDetailsFactory.create(errorCode, detail, request));
	}

	private static ResponseEntity<ProblemDetail> response(ProblemDetail problem) {
		return ResponseEntity.status(problem.getStatus())
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	private static String lastPathSegment(jakarta.validation.Path path) {
		return StreamSupport.stream(path.spliterator(), false)
				.reduce((first, second) -> second)
				.map(jakarta.validation.Path.Node::getName)
				.filter(name -> !name.isBlank())
				.orElse("request");
	}
}
