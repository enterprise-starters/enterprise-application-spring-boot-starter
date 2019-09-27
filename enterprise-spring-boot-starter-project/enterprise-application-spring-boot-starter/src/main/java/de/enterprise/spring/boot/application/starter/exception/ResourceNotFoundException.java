package de.enterprise.spring.boot.application.starter.exception;

import de.enterprise.spring.boot.common.exception.TechnicalException;

/**
 * Own implementation for resource not found exception.
 *
 * @author Malte Geßner
 *
 */
public class ResourceNotFoundException extends TechnicalException {

	private static final long serialVersionUID = 7992904489502842099L;

	public ResourceNotFoundException() {
		this("Resource not found!");
	}

	public ResourceNotFoundException(String message) {
		this(null, message);
	}

	public ResourceNotFoundException(String code, String description, Throwable cause) {
		super(code, description, cause);
	}

	public ResourceNotFoundException(String code, String description) {
		super(code, description);
	}
}
