package com.sam.clinic.shared.error;

import com.sam.clinic.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsFactory {

	public ProblemDetail create(ApiErrorCode errorCode, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		problem.setType(errorCode.type());
		problem.setTitle(errorCode.title());
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("errorCode", errorCode.name());
		problem.setProperty("correlationId", CorrelationIdFilter.from(request));
		return problem;
	}
}
