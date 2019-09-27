package de.enterprise.spring.boot.application.starter.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.Data;

public class SpELAssertTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		validatorFactory.close();
	}

	@Test
	void violation() {
		TestData testData = new TestData();
		Set<ConstraintViolation<TestData>> violations = validator.validate(testData);
		assertThat(violations)
				.isNotNull()
				.hasSize(1)
				.allMatch(c -> c.getMessage().equals("Either field1 or field2 must be not null"));
	}

	@Test
	void no_violation() {
		TestData testData = new TestData();
		testData.setField1("notNull");
		Set<ConstraintViolation<TestData>> violations = validator.validate(testData);
		assertThat(violations)
				.isNotNull()
				.isEmpty();
	}

	@Data
	@SpELAssert(value = "field1 != null || field2 != null", message = "Either field1 or field2 must be not null")
	public class TestData {

		private String field1;
		private String field2;

	}

}
