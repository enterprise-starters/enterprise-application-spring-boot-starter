package de.enterprise.spring.boot.application.starter.actuate.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 *
 * @author Malte Geßner
 *
 */
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnProperty(name = "enterprise-application.actuator.endpoint-security-enabled", havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class ServletActuatorSecurityConfiguration {

	@Configuration(proxyBeanMethods = false)
	public class ActuatorServletSecurityConfiguration extends WebSecurityConfigurerAdapter {
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
			http.requestMatchers()
					.requestMatchers(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint()).and()
					.authorizeRequests()
					.requestMatchers(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
							.to(HealthEndpoint.class, InfoEndpoint.class))
					.permitAll()
					.requestMatchers(org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint())
					.hasAuthority("MANAGE_ADMIN")
					.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
					.and().csrf().disable().httpBasic();
		}
	}

	/**
	 * Security configuration.
	 *
	 * @author Jonas Keßler
	 */
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@Order(99)
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true", matchIfMissing = false)
	public class H2ConsoleSecurityConfig extends WebSecurityConfigurerAdapter {
		/**
		 * H2ConsoleProperties are empty if not explicitly configured and spring-boot-dev-tools activated.
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
