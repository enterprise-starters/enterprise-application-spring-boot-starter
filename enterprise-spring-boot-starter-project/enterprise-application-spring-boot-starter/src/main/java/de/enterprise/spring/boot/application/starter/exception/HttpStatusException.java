package de.enterprise.spring.boot.application.starter.exception;

import org.springframework.http.HttpStatus;

import de.enterprise.spring.boot.common.exception.TechnicalException;

/**
 *
 * @author Jonas Keßler
 */
public abstract class HttpStatusException extends TechnicalException {

	private static final long serialVersionUID = 1L;

	public HttpStatusException(String code, String description, Throwable cause) {
		super(code, description, cause);
	}

	public HttpStatusException(String code, String description) {
		super(code, description);
	}

	public HttpStatusException(final String message) {
		super(message);
	}

	public abstract HttpStatus getHttpStatus();

}
