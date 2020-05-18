package de.enterprise.spring.boot.application.starter.exception;

import org.springframework.http.HttpStatus;

/**
 * Own implementation to handle bad requests.
 *
 * @author Malte Geßner
 *
 */
public class BadRequestException extends HttpStatusException {

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

	@Override
	public HttpStatus getHttpStatus() {
		return HttpStatus.BAD_REQUEST;
	}

}
