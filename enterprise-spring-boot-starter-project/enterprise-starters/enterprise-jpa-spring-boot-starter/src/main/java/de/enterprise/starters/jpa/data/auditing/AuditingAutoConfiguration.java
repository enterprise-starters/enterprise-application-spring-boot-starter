package de.enterprise.starters.jpa.data.auditing;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

/**
 * bind security context for spring data auditing mechanismn.
 * 
 * @author Malte Geßner
 *
 */
@Configuration
public class AuditingAutoConfiguration {

	@ConditionalOnClass(AuditorAware.class)
	static class SpringSecurityAuditorAwareConfiguration {
		@Bean
		SpringSecurityAuditorAware springSecurityAuditorAware() {
			return new SpringSecurityAuditorAware();
		}

		@Bean
		public DateTimeProvider zonedDateTimeProvider() {
			return new DateTimeProvider() {
				@Override
				public Optional<TemporalAccessor> getNow() {
					return Optional.of(ZonedDateTime.now());
				}
			};
		}
	}
}
