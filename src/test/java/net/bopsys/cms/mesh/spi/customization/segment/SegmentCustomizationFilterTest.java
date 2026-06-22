package net.bopsys.cms.mesh.spi.customization.segment;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.test.StepVerifier;

import java.util.Map;

/**
 * Tests for SegmentCustomizationFilter confirming vip exclusive targeting.
 *
 * @author Marco Ruiz
 */
class SegmentCustomizationFilterTest {

	private final SegmentCustomizationFilter filter = new SegmentCustomizationFilter();

	@Test
	void applyCustomization_shouldMutateComponentState_whenVip() {
		ContentComponent component = ContentComponent.builder()
				.componentId("/test")
				.type("text/base")
				.state(Map.of("title", "Hello World", "count", 5))
				.build();

		UserContext context = new UserContext(Map.of(ContextKey.TIER, "VIP"));

		StepVerifier.create(filter.applyCustomization(component, context))
				.expectNextMatches(result ->
						"[VIP EXCLUSIVE] Hello World".equals(result.state().get("title")) &&
								Integer.valueOf(5).equals(result.state().get("count"))
				)
				.verifyComplete();
	}

	@ParameterizedTest
	@CsvSource({
			"REGULAR",
			"GUEST",
			"NON_VIP"
	})
	void applyCustomization_shouldNotMutate_whenNotVip(String tier) {
		ContentComponent component = ContentComponent.builder()
				.componentId("/test")
				.type("text/base")
				.state(Map.of("title", "Hello World"))
				.build();

		UserContext context = new UserContext(Map.of(ContextKey.TIER, tier));

		StepVerifier.create(filter.applyCustomization(component, context))
				.expectNextMatches(result ->
						"Hello World".equals(result.state().get("title"))
				)
				.verifyComplete();
	}
}
