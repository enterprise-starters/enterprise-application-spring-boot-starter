package de.enterprise.spring.boot.application.starter.actuate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.enterprise.spring.boot.application.starter.actuate.endpoint.DocumentationMvcEndpoint;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MeterRegistry.Config;

/**
 *
 * @author Malte Geßner
 *
 */
@Configuration
@EnableConfigurationProperties(ActuatorProperties.class)
public class ActuatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public DocumentationMvcEndpoint documentationMvcEndpoint() {
		return new DocumentationMvcEndpoint();
	}

	@Bean
	MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment, ActuatorProperties actuatorProperties) {
		return registry -> {
			Config config = registry.config().commonTags("profiles", StringUtils.join(environment.getActiveProfiles(), ","));

			if (StringUtils.isNotBlank(actuatorProperties.getInstanceCommonTagValue())) {
				config.commonTags("instance", actuatorProperties.getInstanceCommonTagValue());
			}

			if (StringUtils.isNotBlank(actuatorProperties.getVersionCommonTagValue())) {
				config.commonTags("version", actuatorProperties.getVersionCommonTagValue());
			}
		};
	}

	@Bean
	@ConditionalOnMissingBean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Configuration
	public class ActuatorSecurityConfiguration extends WebSecurityConfigurerAdapter {
		@Autowired
		private SecurityProperties securityProperties;

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser(this.securityProperties.getUser().getName())
					.password(this.securityProperties.getUser().getPassword())
					.authorities(this.securityProperties.getUser().getRoles().toArray(new String[] {}));
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
					.requestMatchers(EndpointRequest.toAnyEndpoint()).hasAuthority("MANAGE_ADMIN")
					.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
					.and().csrf().disable().httpBasic();
		}
	}

	/**
	 * Security configuration.
	 *
	 * @author Jonas Keßler
	 */
	@Order(99)
	@Configuration
	public class H2ConsoleSecurityConfig extends WebSecurityConfigurerAdapter {
		/**
		 * H2ConsoleProperties are empty if not explicitly configured and spring-boot-dev-tools aktivated.
		 */
		@Autowired(required = false)
		private H2ConsoleProperties h2ConsoleProperties;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().disable().headers().frameOptions().disable();
			String h2ConsolePath = this.h2ConsoleProperties != null ? this.h2ConsoleProperties.getPath() : "/h2-console";

			http.requestMatchers().antMatchers(h2ConsolePath, h2ConsolePath + "/**").and().authorizeRequests().anyRequest().anonymous();
		}
	}
}
