package net.bopsys.cms.mesh.util;

import lombok.extern.slf4j.Slf4j;
import net.bopsys.cms.mesh.model.CmsConstants;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayout;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Provides static fallback constructors on resolution failures.
 *
 * @author Marco Ruiz
 */
@Slf4j
public final class ProviderDriverFallbackProvider {

	private ProviderDriverFallbackProvider() {}

	/**
	 * Constructs a default fallback PageLayout when a page layout resolver fails.
	 *
	 * @param pagePath the path to the resolved page
	 * @param error    causing exception
	 * @return a Mono emitting a non-null placeholder PageLayout
	 */
	public static Mono<PageLayout> fallbackLayout(String pagePath, Throwable error) {
		log.error("Layout resolution failed for page path: {}", pagePath, error);
		PageLayoutDefinition def = PageLayoutDefinition.builder()
				.pagePath(pagePath)
				.templateName(CmsConstants.FALLBACK_TEMPLATE)
				.slots(Map.of())
				.build();
		return Mono.just(PageLayout.builder()
				.definition(def)
				.slots(Map.of())
				.build());
	}

	/**
	 * Constructs a default fallback PageLayoutDefinition when a page layout resolver fails.
	 *
	 * @param pagePath the path to the resolved page
	 * @param error    causing exception
	 * @return a Mono emitting a non-null placeholder PageLayoutDefinition
	 */
	public static Mono<PageLayoutDefinition> fallbackLayoutDefinition(String pagePath, Throwable error) {
		log.error("Layout definition resolution failed for page path: {}", pagePath, error);
		return Mono.just(PageLayoutDefinition.builder()
				.pagePath(pagePath)
				.templateName(CmsConstants.FALLBACK_TEMPLATE)
				.slots(Map.of())
				.build());
	}

	/**
	 * Constructs a fallback ContentComponent on component resolver failure.
	 *
	 * @param componentPath the path to the resolved component
	 * @param error         the causing throwable
	 * @return a Mono emitting a non-null placeholder ContentComponent
	 */
	public static Mono<ContentComponent> fallbackComponent(String componentPath, Throwable error) {
		String msg = error != null ? error.getMessage() : "Unknown resolution failure";
		return Mono.just(ContentComponent.builder()
				.componentId(componentPath)
				.type(CmsConstants.FALLBACK_TYPE)
				.state(Map.of(
						CmsConstants.KEY_ERROR, true,
						"errorMessage", msg != null ? msg : "No error message provided"
				))
				.build());
	}
}
