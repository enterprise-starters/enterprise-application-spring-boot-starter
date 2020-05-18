package de.enterprise.spring.boot.application.starter.exception.testapp;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Max;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.enterprise.spring.boot.application.starter.exception.BadRequestException;
import de.enterprise.spring.boot.application.starter.exception.ConstraintViolationValidationException;
import de.enterprise.spring.boot.application.starter.exception.ResourceNotFoundException;
import de.enterprise.spring.boot.application.starter.exception.ValidationException;
import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Malte Geßner
 */
@RestController
public class ExceptionTestController {

	@Autowired
	private Validator validator;

	@GetMapping(value = "/exceptions", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
	public void notFoundException(@RequestParam String exception, @RequestParam(required = false) String code,
			@RequestParam(required = false) String message) throws ValidationException {
		switch (exception) {
			case "BadRequestException":
				throw new BadRequestException(code, message);
			case "AccessDeniedException":
				throw new AccessDeniedException(message);
			case "ResourceNotFoundException":
				throw new ResourceNotFoundException(code, message);
			case "ValidationException":
				throw new ValidationException(code, message);
			case "TechnicalException":
				throw new TechnicalException(code, message);
			case "RuntimeException":
				throw new RuntimeException(message);
		}
	}

	// validation via @Valid
	@SuppressWarnings("unused")
	@PostMapping("/beanValidation")
	public String beanValidation(@RequestBody @Valid DataContainer dataContainer) {
		return "notvalid";
	}

	// validation via Validator
	@PostMapping("/beanValidation2")
	public String beanValidation2(@RequestBody DataContainer dataContainer) throws ConstraintViolationValidationException {
		Set<ConstraintViolation<DataContainer>> violations = this.validator.validate(dataContainer);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationValidationException("custom code", violations);
		}
		return "notvalid";
	}

	@SuppressWarnings("unused")
	@GetMapping("/requireParameter")
	public String beanValidation(@RequestParam("someParam") String someParam) {
		return "notvalid";
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DataContainer {
		@Max(10)
		private Integer count;

		private Status status;

		public enum Status {
			NEW, OLD;
		}
	}

}
