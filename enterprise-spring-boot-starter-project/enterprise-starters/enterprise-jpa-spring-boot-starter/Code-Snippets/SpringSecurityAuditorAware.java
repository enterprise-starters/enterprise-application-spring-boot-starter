package de.enterprise.starters.jpa.auditing;

import java.util.Optional;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This class is needed to use {@link CreatedBy} or {@link LastModifiedBy} annotations in entities.
 *
 * @author Jonas Keßler
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		return Optional.of(authentication.getPrincipal().toString());
	}
}
