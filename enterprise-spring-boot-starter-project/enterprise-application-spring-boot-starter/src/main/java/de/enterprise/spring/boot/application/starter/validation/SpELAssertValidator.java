package de.enterprise.spring.boot.application.starter.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * ConstraintValidator implementation for custom validation annotation {@link SpELAssert}.
 *
 * @author Jonas Keßler
 */
public class SpELAssertValidator implements ConstraintValidator<SpELAssert, Object> {

	private Expression exp;

	@Override
	public void initialize(SpELAssert annotation) {
		ExpressionParser parser = new SpelExpressionParser();
		this.exp = parser.parseExpression(annotation.value());
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		return this.exp.getValue(value, Boolean.class);
	}
}
