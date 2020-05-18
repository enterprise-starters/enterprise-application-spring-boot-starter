package de.enterprise.spring.boot.application.starter.exception;

import de.enterprise.spring.boot.common.exception.TechnicalException;

/**
 * Special exception should be throw everyone a service is not available, such like timeout, connection refused, etc.
 *
 * @author Malte Geßner
 *
 */
public class ServiceUnavailableException extends TechnicalException {

	private static final long serialVersionUID = -7962247200341748510L;

	public ServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
