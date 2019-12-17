package de.enterprise.spring.boot.application.starter.exception.rest;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import de.enterprise.spring.boot.application.starter.exception.BadRequestException;
import de.enterprise.spring.boot.application.starter.exception.ConstraintViolationValidationException;
import de.enterprise.spring.boot.application.starter.exception.DataBindingValidationException;
import de.enterprise.spring.boot.application.starter.exception.ExceptionWrapper;
import de.enterprise.spring.boot.application.starter.exception.HttpStatusException;
import de.enterprise.spring.boot.application.starter.exception.ResourceNotFoundException;
import de.enterprise.spring.boot.application.starter.exception.ValidationException;
import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handling for all global defined exceptions.
 *
 * TODO: improve documentation
 *
 * @author Malte Geßner
 *
 */
@ControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionRestControllerHandler {

	@ExceptionHandler(HttpStatusException.class)
	public ResponseEntity<ExceptionWrapper> handleBadRequest(HttpStatusException ex, HttpServletRequest servletRequest) {
		HttpStatus status = ex.getHttpStatus();
		log.debug(logMessageFromStatusAndException(status), ex);
		return new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(servletRequest), status);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ExceptionWrapper> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest servletRequest) {
		HttpStatus status = HttpStatus.FORBIDDEN;
		log.error(logMessageFromStatusAndException(status), ex);
		return new ResponseEntity<>(new ExceptionWrapper(ex, String.valueOf(status.value())), defaultHeaders(servletRequest), status);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex, HttpServletRequest servletRequest) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		log.debug(logMessageFromStatusAndException(status), ex);
		if (ex.getCode() == null) {
			return new ResponseEntity<>(status);
		}
		return new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(servletRequest), status);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ExceptionWrapper> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest servletRequest) {
		return this.handleValidationException(
				new DataBindingValidationException("900", ex.getParameter().getParameterName(), ex.getBindingResult()), servletRequest);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ExceptionWrapper> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
			HttpServletRequest servletRequest) {
		return this.handleBadRequest(new BadRequestException("901", ex.getMessage(), ex), servletRequest);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ExceptionWrapper> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex,
			HttpServletRequest servletRequest) {
		return this.handleValidationException(new ValidationException("902", ex.getMessage()), servletRequest);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ExceptionWrapper> handleValidationException(ValidationException ex, HttpServletRequest servletRequest) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
		log.warn(logMessageFromStatusAndException(status), ex);

		ExceptionWrapper exceptionWrapper = new ExceptionWrapper(ex);

		if (ex instanceof DataBindingValidationException) {
			Errors errors = ((DataBindingValidationException) ex).getErrors();
			if (errors != null) {
				Map<String, String> validationFieldErrors = new HashMap<>();
				errors.getFieldErrors().forEach(fieldError -> {
					validationFieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
				});
				exceptionWrapper.setValues(validationFieldErrors);
			}
		} else if (ex instanceof ConstraintViolationValidationException) {
			Set<? extends ConstraintViolation<?>> constraintViolations = ((ConstraintViolationValidationException) ex)
					.getConstraintViolations();
			if (constraintViolations != null) {
				Map<String, String> validationFieldErrors = new HashMap<>();
				constraintViolations.forEach(fieldError -> {
					validationFieldErrors.put(fieldError.getPropertyPath().toString(), fieldError.getMessage());
				});
				exceptionWrapper.setValues(validationFieldErrors);
			}
		}

		return new ResponseEntity<>(exceptionWrapper, defaultHeaders(servletRequest), status);
	}

	@ExceptionHandler(TechnicalException.class)
	public ResponseEntity<ExceptionWrapper> handleTechnicalException(TechnicalException ex, HttpServletRequest servletRequest) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		log.error(logMessageFromStatusAndException(status), ex);
		return new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(servletRequest), status);
	}

	// TODO: this prevents all standard exception handling of springs DefaultHandlerExceptionResolver. Do we really want that?
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ExceptionWrapper> handleException(Exception ex, HttpServletRequest servletRequest) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		log.error(logMessageFromStatusAndException(status), ex);
		return new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(servletRequest), status);
	}

	private String logMessageFromStatusAndException(HttpStatus responseStatus) {
		return "Returning " + responseStatus.value() + " - " + responseStatus.getReasonPhrase() + ". Exception caught:";
	}

	private HttpHeaders defaultHeaders(HttpServletRequest servletRequest) {
		boolean applicationXmlFound = containsHeaderValue(servletRequest, MediaType.APPLICATION_XML_VALUE);

		HttpHeaders headers = new HttpHeaders();
		if (applicationXmlFound) {
			headers.setContentType(MediaType.APPLICATION_XML);
		} else {
			headers.setContentType(MediaType.APPLICATION_JSON);
		}
		return headers;
	}

	private boolean containsHeaderValue(HttpServletRequest servletRequest, String mediaTypeValue) {
		boolean acceptHeaderFound = false;
		Enumeration<String> acceptHeaderValues = servletRequest.getHeaders(HttpHeaders.ACCEPT);
		while (acceptHeaderValues.hasMoreElements()) {
			String nextElement = acceptHeaderValues.nextElement();
			if (nextElement != null) {
				acceptHeaderFound = acceptHeaderFound
						|| mediaTypeValue.equals(nextElement)
						|| checkCommaSeparatedHeader(nextElement, mediaTypeValue);
			}
		}
		return acceptHeaderFound;
	}

	private boolean checkCommaSeparatedHeader(String headerContent, String searchTerm) {
		boolean result = false;
		String[] splitted = headerContent.split(",");
		for (String s : splitted) {
			result = result || s.equals(searchTerm);
		}
		return result;
	}
}
