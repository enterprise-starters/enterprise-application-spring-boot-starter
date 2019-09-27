package de.enterprise.spring.boot.application.starter.actuate.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Manage Endpoint for service documentations.
 *
 * @author Malte Geßner
 *
 */
@Endpoint(id = "api")
public class DocumentationMvcEndpoint {

	private Resource apiHtmlDocument;
	private Resource apiSwaggerJsonDocument;

	public DocumentationMvcEndpoint() {
		this.apiHtmlDocument = new ClassPathResource("/docs/api.html");
		this.apiSwaggerJsonDocument = new ClassPathResource("/docs/swagger.json");
	}

	@ReadOperation(produces = MediaType.TEXT_HTML_VALUE)
	public @ResponseBody Resource getApi() {
		return this.apiHtmlDocument;
	}

	@ReadOperation(produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Resource getApiSwaggerJson() {
		return this.apiSwaggerJsonDocument;
	}
}