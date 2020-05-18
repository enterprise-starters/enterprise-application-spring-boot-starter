package de.enterprise.spring.boot.application.starter.application;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Central properties class for application starter.
 *
 * @author Malte Geßner
 *
 */

@ConfigurationProperties(prefix = "enterprise-application")
@Getter
@Setter
@Validated
public class EnterpriseApplicationProperties {

	@NotNull
	@Valid
	private Application application;
	@NotNull
	@Valid
	private Project project;

	/**
	 * Properties for application base.
	 *
	 * @author Malte Geßner
	 *
	 */
	@Getter
	@Setter
	@Validated
	public static class Application {
		@NotNull
		private String name;
	}

	/**
	 * General meta project properties.
	 *
	 * @author Malte Geßner
	 *
	 */
	@Getter
	@Setter
	@Validated
	public static class Project {
		@NotNull
		private String name;
		@NotNull
		private String artifactId;
		@NotNull
		private String groupId;
		@NotNull
		private String version;
		private String description;
	}
}
