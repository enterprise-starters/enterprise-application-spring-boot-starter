package de.enterprise.spring.boot.application.starter.exception.rest.reactive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import de.enterprise.spring.boot.application.starter.exception.BadRequestException;
import de.enterprise.spring.boot.application.starter.exception.ConstraintViolationValidationException;
import de.enterprise.spring.boot.application.starter.exception.DataBindingValidationException;
import de.enterprise.spring.boot.application.starter.exception.ExceptionWrapper;
import de.enterprise.spring.boot.application.starter.exception.HttpStatusException;
import de.enterprise.spring.boot.application.starter.exception.ResourceNotFoundException;
import de.enterprise.spring.boot.application.starter.exception.ValidationException;
import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Global exception handling for all global defined exceptions.
 *
 * TODO: improve documentation
 *
 * @author Malte Geßner
 *
 */
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionRestControllerHandler {

	@ExceptionHandler(HttpStatusException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleBadRequest(HttpStatusException ex, ServerWebExchange serverWebExchange) {
		HttpStatus status = ex.getHttpStatus();
		log.debug(logMessageFromStatusAndException(status), ex);
		return Mono.just(new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(serverWebExchange.getRequest()), status));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleAccessDeniedException(AccessDeniedException ex,
			ServerWebExchange serverWebExchange) {
		HttpStatus status = HttpStatus.FORBIDDEN;
		log.error(logMessageFromStatusAndException(status), ex);
		return Mono.just(new ResponseEntity<>(new ExceptionWrapper(ex, String.valueOf(status.value())),
				defaultHeaders(serverWebExchange.getRequest()), status));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public Mono<ResponseEntity<?>> handleNotFound(ResourceNotFoundException ex, ServerWebExchange serverWebExchange) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		log.debug(logMessageFromStatusAndException(status), ex);
		if (ex.getCode() == null) {
			return Mono.just(new ResponseEntity<>(status));
		}
		return Mono.just(new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(serverWebExchange.getRequest()), status));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			ServerWebExchange serverWebExchange) {
		return this.handleValidationException(
				new DataBindingValidationException("900", ex.getParameter().getParameterName(), ex.getBindingResult()),
				serverWebExchange);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
			ServerWebExchange serverWebExchange) {
		return this.handleBadRequest(new BadRequestException("901", ex.getMessage(), ex), serverWebExchange);
	}

	@ExceptionHandler(ValidationException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleValidationException(ValidationException ex, ServerWebExchange serverWebExchange) {
		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
		log.warn(logMessageFromStatusAndException(status), ex);

		ExceptionWrapper exceptionWrapper = new ExceptionWrapper(ex);

		if (ex instanceof DataBindingValidationException) {
			Errors errors = ((DataBindingValidationException) ex).getErrors();
			if (errors != null) {
				Map<String, Object> validationFieldErrors = new HashMap<>();
				errors.getFieldErrors().forEach(fieldError -> {
					validationFieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
				});
				exceptionWrapper.setValues(validationFieldErrors);
			}
		} else if (ex instanceof ConstraintViolationValidationException) {
			Set<? extends ConstraintViolation<?>> constraintViolations = ((ConstraintViolationValidationException) ex)
					.getConstraintViolations();
			if (constraintViolations != null) {
				Map<String, Object> validationFieldErrors = new HashMap<>();
				constraintViolations.forEach(fieldError -> {
					validationFieldErrors.put(fieldError.getPropertyPath().toString(), fieldError.getMessage());
				});
				exceptionWrapper.setValues(validationFieldErrors);
			}
		}

		return Mono.just(new ResponseEntity<>(exceptionWrapper, defaultHeaders(serverWebExchange.getRequest()), status));
	}

	@ExceptionHandler(TechnicalException.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleTechnicalException(TechnicalException ex, ServerWebExchange serverWebExchange) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		log.error(logMessageFromStatusAndException(status), ex);
		return Mono.just(new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(serverWebExchange.getRequest()), status));
	}

	// TODO: this prevents all standard exception handling of springs DefaultHandlerExceptionResolver. Do we really want that?
	@ExceptionHandler(Exception.class)
	public Mono<ResponseEntity<ExceptionWrapper>> handleException(Exception ex, ServerWebExchange serverWebExchange) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		log.error(logMessageFromStatusAndException(status), ex);
		return Mono.just(new ResponseEntity<>(new ExceptionWrapper(ex), defaultHeaders(serverWebExchange.getRequest()), status));
	}

	public static String logMessageFromStatusAndException(HttpStatus responseStatus) {
		return "Returning " + responseStatus.value() + " - " + responseStatus.getReasonPhrase() + ". Exception caught:";
	}

	public static HttpHeaders defaultHeaders(ServerHttpRequest serverHttpRequest) {
		boolean applicationXmlFound = containsHeaderValue(serverHttpRequest, MediaType.APPLICATION_XML_VALUE);

		HttpHeaders headers = new HttpHeaders();
		if (applicationXmlFound) {
			headers.setContentType(MediaType.APPLICATION_XML);
		} else {
			headers.setContentType(MediaType.APPLICATION_JSON);
		}
		return headers;
	}

	public static boolean containsHeaderValue(ServerHttpRequest serverHttpRequest, String mediaTypeValue) {
		List<String> acceptHeaderValues = serverHttpRequest.getHeaders().get(HttpHeaders.ACCEPT);

		return acceptHeaderValues.stream().anyMatch(headerValue -> StringUtils.containsIgnoreCase(headerValue, mediaTypeValue));
	}
}
