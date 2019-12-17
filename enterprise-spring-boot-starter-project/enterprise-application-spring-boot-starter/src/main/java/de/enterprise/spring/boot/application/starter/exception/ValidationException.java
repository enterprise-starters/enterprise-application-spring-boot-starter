package de.enterprise.spring.boot.application.starter.exception;

import de.enterprise.spring.boot.common.exception.BusinessException;

/**
 * Special validation exception, use this exception for all validation exceptions in controller and service classes.
 *
 * @author Malte Geßner
 *
 */
public class ValidationException extends BusinessException {

	private static final long serialVersionUID = 201208100801L;

	public ValidationException(final String code, final String description, Throwable t) {
		super(code, description, t);
	}

	public ValidationException(final String code, final String description) {
		super(code, description);
	}

}
