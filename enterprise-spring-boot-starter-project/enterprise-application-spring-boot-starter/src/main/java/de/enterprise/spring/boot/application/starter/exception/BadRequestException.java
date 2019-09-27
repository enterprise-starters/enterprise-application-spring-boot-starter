package de.enterprise.spring.boot.application.starter.exception;

import de.enterprise.spring.boot.common.exception.TechnicalException;

/**
 * Own implementation to handle bad requests.
 * 
 * @author Malte Geßner
 *
 */
public class BadRequestException extends TechnicalException {

	private static final long serialVersionUID = -8117851683986681378L;

	public BadRequestException() {
		this("Bad request!");
	}

	public BadRequestException(String message) {
		this(null, message);
	}

	public BadRequestException(String code, String description, Throwable cause) {
		super(code, description, cause);
	}

	public BadRequestException(String code, String description) {
		super(code, description);
	}

}
