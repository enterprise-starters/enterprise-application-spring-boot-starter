package de.enterprise.spring.boot.application.starter.exception;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.enterprise.spring.boot.common.exception.BusinessException;
import de.enterprise.spring.boot.common.exception.TechnicalException;
import lombok.Data;

/**
 * 
 * @author Malte Geßner
 *
 */
@Data
@JsonInclude(Include.NON_NULL)
public class ExceptionWrapper {

	private String code;
	private String message;
	private Map<String, Object> values;

	public ExceptionWrapper(Exception ex) {
		this.message = ex.getMessage();
		this.code = "500";
	}

	public ExceptionWrapper(Exception ex, String code) {
		this.message = ex.getMessage();
		this.code = code;
	}

	public ExceptionWrapper(TechnicalException ex) {
		this.message = ex.getDescription();
		this.code = ex.getCode();
	}

	public ExceptionWrapper(BusinessException ex) {
		this.message = ex.getDescription();
		this.code = ex.getCode();
	}
}
