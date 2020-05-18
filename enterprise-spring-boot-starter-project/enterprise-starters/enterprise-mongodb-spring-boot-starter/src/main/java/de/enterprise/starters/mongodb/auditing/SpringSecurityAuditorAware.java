package de.enterprise.starters.mongodb.auditing;

import java.util.Optional;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * This class is needed to use {@link CreatedBy} annotation in entities.
 *
 * @author Malte Geßner
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		if (authentication.getPrincipal() instanceof UserDetails) {
			return Optional.of(((UserDetails) authentication.getPrincipal()).getUsername());
		}

		return Optional.of(authentication.getPrincipal().toString());
	}
}
