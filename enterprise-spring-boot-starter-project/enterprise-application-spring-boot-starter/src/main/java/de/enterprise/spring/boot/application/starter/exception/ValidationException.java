package de.enterprise.spring.boot.application.starter.exception;

import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import de.enterprise.spring.boot.common.exception.BusinessException;

/**
 * Special validation exception, use this exception for all validation exceptions in controller and service classes.
 * 
 * @author Malte Geßner
 *
 */
public class ValidationException extends BusinessException {

	private static final long serialVersionUID = 201208100801L;
	private static final String SPACE = " ";

	private final Errors errors;

	public ValidationException(final String code, final String description, final Errors pErrors) {
		super(code, description);
		this.errors = pErrors;
	}

	public ValidationException(final String code, final String description) {
		super(code, description);
		this.errors = null;
	}

	public ValidationException(Errors errors) {
		super("internal.validation.error", errors.toString());
		this.errors = errors;
	}

	public Errors getErrors() {
		return this.errors;
	}

	@Override
	public String getMessage() {
		if (this.errors == null || !this.errors.hasErrors()) {
			return super.getMessage();
		}
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(super.getMessage()).append(" -");
		for (ObjectError error : this.errors.getAllErrors()) {
			stringBuilder.append(SPACE);
			if (error instanceof FieldError) {
				stringBuilder
						.append("Field '")
						.append(((FieldError) error).getField())
						.append("'");
			} else {
				// TODO gibt es diesen Fall???
				stringBuilder.append(error.getCode());
			}
			stringBuilder
					.append(": ")
					.append(error.getDefaultMessage())
					.append(";");
		}
		return stringBuilder.toString();
	}

}
