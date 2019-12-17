package de.enterprise.spring.boot.application.starter.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validation annotation which can be used to evaluate objects with spring expression language (SpEL) expressions. Accepts any type.
 *
 * @author Jonas Keßler
 */
@Constraint(validatedBy = SpELAssertValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SpELAsserts.class)
public @interface SpELAssert {
	String message() default "SpEL expression must evaluate to true";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String value();
}
