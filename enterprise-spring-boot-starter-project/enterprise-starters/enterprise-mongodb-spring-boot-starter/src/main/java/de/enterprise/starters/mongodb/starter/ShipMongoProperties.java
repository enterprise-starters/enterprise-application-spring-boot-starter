package de.enterprise.starters.mongodb.starter;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

/**
 * 
 * @author Malte Geßner
 *
 */
@ConfigurationProperties(prefix = "ship.mongodb", ignoreUnknownFields = false)
@Validated
@Data
public class ShipMongoProperties {

	@NotNull
	private String password;

	@NotNull
	private String namespace;
}
