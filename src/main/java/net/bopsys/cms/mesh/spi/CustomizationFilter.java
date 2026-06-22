package net.bopsys.cms.mesh.spi;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.UserContext;
import reactor.core.publisher.Mono;

/**
 * SPI for pluggable customization filters.
 *
 * @author Marco Ruiz
 */
public interface CustomizationFilter {

	/**
	 * Applies customization logic to a component.
	 *
	 * @param incoming    the incoming component
	 * @param ctx the user context wrapper
	 * @return Mono of the customized component
	 */
	Mono<ContentComponent> applyCustomization(ContentComponent incoming, UserContext ctx);
}
