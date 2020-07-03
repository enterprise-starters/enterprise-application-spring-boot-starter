package de.enterprise.spring.boot.application.starter.application;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import de.enterprise.spring.boot.common.exception.TechnicalException;

/**
 * Base class to start new applications. Sets some default values (see /META-INF/application-default.properties) for applications.
 *
 * @author Malte Geßner
 *
 */
@SpringBootApplication
@EnableConfigurationProperties(EnterpriseApplicationProperties.class)
public abstract class AbstractEnterpriseApplication {

	/**
	 * Profile for tests.
	 */
	public static final String INTEGRATION_TEST_PROFILE = "integrationtest";

	protected SpringApplicationBuilder configureApplication(SpringApplicationBuilder application) {

		Properties defaultProperties = new Properties();
		try {
			ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
			Resource[] defaultPropertiesResources = patternResolver.getResources("classpath*:/META-INF/*application-default.properties");

			for (Resource defaultPropertiesResource : defaultPropertiesResources) {
				defaultProperties.load(defaultPropertiesResource.getInputStream());
			}
		} catch (IOException e) {
			throw new TechnicalException("no default properties found!", e);
		}

		return application.bannerMode(Mode.LOG).properties("spring.profiles.default=dev,dev-local," + System.getenv().get("COMPUTERNAME"))
				.properties(defaultProperties);
	}

	protected void run() {
		this.configureApplication(new SpringApplicationBuilder(this.getClass())).application().run();
	}

	/**
	 * Extra default properties handling only for integration tests. The configureApplication method isn't used/called in IntegrationTests.
	 * The PropertySource mechanism is to late for show banner process. So we use the classic Properties mechanism for default properties.
	 *
	 * @author Malte Geßner
	 *
	 */
	@Configuration
	@Profile(INTEGRATION_TEST_PROFILE)
	@PropertySource("classpath:/META-INF/application-default.properties")
	public static class DefaultProperties {

	}
}
