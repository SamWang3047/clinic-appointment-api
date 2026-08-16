package com.sam.clinic.shared.error;

public class BusinessRuleException extends ApiException {

	public BusinessRuleException(String message) {
		super(ApiErrorCode.BUSINESS_RULE_VIOLATION, message);
	}
}
