package de.enterprise.spring.boot.common.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * There was a technical problem that prevented the operation from completing.
 *
 * @author Malte Geßner
 */
public class TechnicalException extends RuntimeException {

	private static final long serialVersionUID = 201109130948L;

	/** known by gui. */
	private final String code;
	/** debug info for developer. */
	private final String description;

	/**
	 * message and cause.
	 *
	 * @param code
	 *            message
	 * @param description
	 *            description
	 * @param cause
	 *            cause
	 */
	public TechnicalException(final String code, final String description, final Throwable cause) {
		super(StringUtils.defaultIfBlank(code, "") + " " + StringUtils.trimToEmpty(description), cause);
		this.code = code;
		this.description = description;
	}

	public TechnicalException(final String code, final String description) {
		super(code + " " + description);
		this.code = code;
		this.description = description;
	}

	public TechnicalException(final String message) {
		super(message);
		this.code = null;
		this.description = message;
	}

	public TechnicalException(final String message, final Throwable cause) {
		super(message, cause);
		this.code = null;
		this.description = message;
	}

	public String getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}
}
