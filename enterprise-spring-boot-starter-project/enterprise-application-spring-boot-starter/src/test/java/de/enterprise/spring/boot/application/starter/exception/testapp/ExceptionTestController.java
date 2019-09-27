package de.enterprise.spring.boot.application.starter.exception.testapp;

import javax.validation.Valid;
import javax.validation.constraints.Max;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.enterprise.spring.boot.application.starter.exception.BadRequestException;
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

	@SuppressWarnings("unused")
	@PostMapping("/beanValidation")
	public String beanValidation(@RequestBody @Valid DataContainer dataContainer) {
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
