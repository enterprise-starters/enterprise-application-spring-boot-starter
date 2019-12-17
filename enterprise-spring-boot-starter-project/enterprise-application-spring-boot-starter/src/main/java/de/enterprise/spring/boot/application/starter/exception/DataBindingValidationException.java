package de.enterprise.spring.boot.application.starter.exception;

import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import lombok.Getter;

/**
 * Special case of {@link ValidationException}. Holds an {@link Errors} object which stores and exposes information about data-binding and
 * validation errors for a specific object.
 *
 * @author Jonas Keßler
 */
@Getter
public class DataBindingValidationException extends ValidationException {

	private static final long serialVersionUID = 1L;
	private static final String SPACE = " ";

	private final Errors errors;

	public DataBindingValidationException(final String code, final String description, final Errors pErrors) {
		super(code, description);
		this.errors = pErrors;
	}

	public DataBindingValidationException(Errors errors) {
		super("internal.validation.error", errors.toString());
		this.errors = errors;
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
