package net.bopsys.cms.mesh.spi;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import reactor.core.publisher.Mono;

/**
 * SPI Provider interface for retrieving content from CMS repositories.
 *
 * @author Marco Ruiz
 */
public interface ProviderDriver {

	/**
	 * Returns the supported provider execution identity.
	 *
	 * @return ProviderDriverProtocol enum identity
	 */
	ProviderDriverProtocol getSupportedProtocol();

	/**
	 * Resolves a content component details.
	 *
	 * @param componentPath component location URL route
	 * @return a Mono emitting the resolved ContentComponent
	 */
	Mono<ContentComponent> resolveComponent(String componentPath);

	/**
	 * Resolves a page layout definitions structure.
	 *
	 * @param pagePath page template location URL route
	 * @return a Mono emitting the resolved PageLayoutDefinition
	 */
	Mono<PageLayoutDefinition> resolvePageLayout(String pagePath);
}
