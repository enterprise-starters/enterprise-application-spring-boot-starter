package de.enterprise.starters.jpa.auditing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/**
 *
 * @author Jonas Keßler
 */

@Configuration
public class AuditingAutoConfiguration {

	@ConditionalOnClass(AuditorAware.class)
	static class SpringSecurityAuditorAwareConfiguration {
		@Bean
		SpringSecurityAuditorAware springSecurityAuditorAware() {
			return new SpringSecurityAuditorAware();
		}
	}
}
