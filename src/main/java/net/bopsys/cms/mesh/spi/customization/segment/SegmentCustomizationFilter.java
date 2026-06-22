package net.bopsys.cms.mesh.spi.customization.segment;

import net.bopsys.cms.mesh.model.CmsConstants;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.CustomizationFilter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Segment customization filter. Applies conditional mutations on VIP users.
 *
 * @author Marco Ruiz
 */
@Component
@Order(1)
public class SegmentCustomizationFilter implements CustomizationFilter {

	@Override
	public Mono<ContentComponent> applyCustomization(ContentComponent incoming, UserContext ctx) {
		return Mono.fromCallable(() -> ctx.is(ContextKey.TIER, CmsConstants.MEMBER_TIER_VIP) ?
				applyVipTransform(incoming) :
				incoming);
	}

	/**
	 * Rebuilds the content component mapping over its current state and mutating textual details.
	 * Guarantees cross-thread consistency using ConcurrentHashMap during manipulation.
	 */
	private ContentComponent applyVipTransform(ContentComponent incoming) {
		Map<String, Object> updatedState = new ConcurrentHashMap<>();
		if (incoming.state() != null) {
			incoming.state().forEach((k, v) -> transformAndPut(updatedState, k, v));
		}
		return incoming.buildCopyWithState(updatedState);
	}

	private void transformAndPut(Map<String, Object> state, String k, Object v) {
		if (k != null && v != null) {
			state.put(k, (v instanceof String) ? CmsConstants.VIP_PREFIX + (String) v : v);
		}
	}
}
