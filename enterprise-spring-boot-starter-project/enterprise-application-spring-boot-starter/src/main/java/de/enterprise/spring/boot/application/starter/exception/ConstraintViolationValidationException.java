package de.enterprise.spring.boot.application.starter.exception;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import lombok.Getter;

/**
 * Special case of {@link ValidationException} which holds a Set of {@link ConstraintViolation} object. Those are created by a
 * {@link Validator}.
 *
 * @author Jonas Keßler
 */
@Getter
public class ConstraintViolationValidationException extends ValidationException {

	private static final long serialVersionUID = 1L;

	private final Set<? extends ConstraintViolation<?>> constraintViolations;

	public ConstraintViolationValidationException(String pCode, Set<? extends ConstraintViolation<?>> constraintViolations) {
		super(pCode, humanReadableViolations(constraintViolations));
		this.constraintViolations = constraintViolations;
	}

	private static String humanReadableViolations(Set<? extends ConstraintViolation<?>> violations) {
		if (violations == null) {
			return "";
		}

		return violations.stream().map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
				.collect(Collectors.joining("\r\n"));
	}

}
