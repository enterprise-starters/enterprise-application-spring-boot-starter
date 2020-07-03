package de.enterprise.spring.boot.application.starter.actuate.reactive;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 *
 * @author Malte Geßner
 *
 */
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnProperty(name = "enterprise-application.actuator.endpoint-security-enabled", havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class ReactiveActuatorSecurityConfiguration {

	@Autowired
	private SecurityProperties securityProperties;

	@Bean
	public MapReactiveUserDetailsService userDetailsService() {
		UserDetails user = User.builder()
				.username(this.securityProperties.getUser().getName())
				.password(this.securityProperties.getUser().getPassword())
				.authorities(
						this.securityProperties.getUser().getRoles().stream().map(SimpleGrantedAuthority::new)
								.collect(Collectors.toList()))
				.build();
		return new MapReactiveUserDetailsService(user);
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http.securityMatcher(org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint());
		http.authorizeExchange((exchanges) -> {
			exchanges.matchers(org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.to(HealthEndpoint.class,
					InfoEndpoint.class)).permitAll();
			exchanges.matchers(org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint())
					.hasAuthority("MANAGE_ADMIN");
			exchanges.matchers(
					org.springframework.boot.autoconfigure.security.reactive.PathRequest.toStaticResources().atCommonLocations())
					.permitAll();
		});
		http.httpBasic();
		http.csrf().disable();
		return http.build();
	}
}
