package de.enterprise.spring.boot.common.exception;

/**
 * The business exception indicates that the arguments provided to a service did not fulfill its requirements. This also includes security
 * violations and a non-existing entity for a primary key. This does not include technical problems.
 *
 * @author Malte Geßner
 *
 */
public class BusinessException extends Exception {
	/** serial uid. */
	public static final long serialVersionUID = 20090825095803L;

	/** known by gui. */
	private final String code;
	/** debug info for developer. */
	private final String description;

	/**
	 * business logic failed.
	 *
	 * @param code
	 *            hint for ui
	 * @param description
	 *            hint for developer
	 */
	public BusinessException(final String code, final String description) {
		super(code + " " + description);
		this.code = code;
		this.description = description;
	}

	/**
	 * business logic failed.
	 *
	 * @param code
	 *            hint for ui
	 * @param description
	 *            hint for developer
	 * @param t
	 *            cause
	 */
	public BusinessException(final String code, final String description, final Throwable t) {
		super(code + " " + description, t);
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}

}
