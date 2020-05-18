package de.enterprise.spring.boot.application.starter.web.singlepage;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.boot.autoconfigure.web.ResourceProperties.Cache.Cachecontrol;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

/**
 *
 * @author Malte Geßner
 *
 */
@ConfigurationProperties(prefix = "enterprise-application.singlepageapplication.ui", ignoreUnknownFields = false)
@Validated
@Data
public class SinglePageApplicationUiProperties {

	@Valid
	private List<SinglePageApplication> singlePageApplications;

	@Validated
	@Data
	public static class SinglePageApplication {
		@NotNull
		private String uiPrefix;
		private String servicePortalPrefix = "";
		@NotNull
		private String resourceLocation;
		private boolean calculateJsModuleName = true;
		private boolean dynamicJsTypeSuffix = true;
		@NotNull
		private String jsModuleNameSuffix = "module-ngfactory";

		@Size(min = 1)
		@Valid
		private List<ResourceConfig> resources;
	}

	@Validated
	@Data
	public static class ResourceConfig {
		@NotNull
		@Size(min = 1)
		private Set<String> pathPattern;
		private boolean cacheUiResources = true;
		private Cachecontrol cacheControl = new Cachecontrol();
	}
}
