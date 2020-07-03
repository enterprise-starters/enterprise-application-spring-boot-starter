package de.enterprise.spring.boot.application.starter.web.singlepage.servlet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import de.enterprise.spring.boot.application.starter.web.singlepage.SinglePageApplicationUiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Malte Geßner
 *
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(SinglePageApplicationUiProperties.class)
public class SinglePageApplicationUiAutoConfiguration {

	@Configuration
	public class ServletWebConfig implements WebMvcConfigurer {
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
									.resourceChain(resourceConfig.isCacheUiResources()).addResolver(new PathResourceResolver() {
										@Override
										protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
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

											return super.resolveResourceInternal(request, usedRequestPath, locations, chain);
										}
									});
						});
					}
				});
			}
		}
	}
}
