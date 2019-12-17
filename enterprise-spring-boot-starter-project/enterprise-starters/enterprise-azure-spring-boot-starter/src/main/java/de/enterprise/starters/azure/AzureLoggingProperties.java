package de.enterprise.starters.azure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Jonas Keßler
 */
@Getter
@Setter
@ConfigurationProperties("enterprise.azure.logging")
public class AzureLoggingProperties {

	@NotNull
	private Boolean applicationInsightsAppenderEnabled = true;

}
