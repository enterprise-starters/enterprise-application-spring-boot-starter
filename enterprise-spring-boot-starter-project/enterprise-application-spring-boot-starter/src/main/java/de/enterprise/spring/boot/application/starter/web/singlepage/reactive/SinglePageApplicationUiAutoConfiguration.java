package de.enterprise.spring.boot.application.starter.web.singlepage.reactive;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;

import de.enterprise.spring.boot.application.starter.web.singlepage.SinglePageApplicationUiProperties;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
@EnableConfigurationProperties(SinglePageApplicationUiProperties.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class SinglePageApplicationUiAutoConfiguration {

	@Configuration
	public class ReactiveWebConfig implements WebFluxConfigurer {
		@Autowired
		private SinglePageApplicationUiProperties singlePageApplicationUiProperties;

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			if (this.singlePageApplicationUiProperties.getSinglePageApplications() != null) {
				this.singlePageApplicationUiProperties.getSinglePageApplications().forEach(singlePageApplication -> {
					if (singlePageApplication.getResources() != null) {
						singlePageApplication.getResources().forEach(resourceConfig -> {

							Set<String> pathPatterns = new LinkedHashSet<>();
							resourceConfig.getPathPattern().forEach(pathPattern -> pathPatterns.add(
									singlePageApplication.getUiPrefix() + singlePageApplication.getServicePortalPrefix() + pathPattern));

							registry.addResourceHandler(pathPatterns.toArray(new String[0]))
									.addResourceLocations(singlePageApplication.getResourceLocation())
									.setCacheControl(resourceConfig.getCacheControl().toHttpCacheControl())
									.resourceChain(resourceConfig.isCacheUiResources())
									.addResolver(new PathResourceResolver() {
										@Override
										protected Mono<Resource> resolveResourceInternal(ServerWebExchange exchange, String requestPath,
												List<? extends Resource> locations, ResourceResolverChain chain) {
											String usedRequestPath = requestPath;
											if (!StringUtils.contains(requestPath, ".")) {
												usedRequestPath = "index.html";
											} else if (singlePageApplication.isCalculateJsModuleName()
													&& StringUtils.endsWith(requestPath, ".js")
													&& StringUtils.contains(requestPath, singlePageApplication.getJsModuleNameSuffix())) {

												String fixModuleName = StringUtils.substringBefore(requestPath,
														singlePageApplication.getJsModuleNameSuffix());
												String moduleType = "";

												if (singlePageApplication.isDynamicJsTypeSuffix()) {
													moduleType = StringUtils.substringBetween(requestPath,
															singlePageApplication.getJsModuleNameSuffix(), ".");
												}

												usedRequestPath = fixModuleName + singlePageApplication.getJsModuleNameSuffix() + moduleType
														+ ".js";
												log.debug("dynamic module name modification. orginal path:{}, new path:{}", requestPath,
														usedRequestPath);
											}

											return super.resolveResourceInternal(exchange, usedRequestPath, locations, chain);
										}
									});
						});
					}
				});
			}
		}
	}
}
