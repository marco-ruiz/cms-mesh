package net.bopsys.cms.mesh.spi.customization.channel;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.customization.channel.ChannelProfile.TransformationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

/**
 * Tests for ChannelCustomizationFilter confirming strategy rule applications.
 *
 * @author Marco Ruiz
 */
class ChannelCustomizationFilterTest {

	private ChannelCustomizationFilter filter;

	@BeforeEach
	void setUp() {
		ChannelProfile mobileProfile = ChannelProfile.builder()
				.channelId("mobile-app")
				.rules(List.of(
						TransformationRule.builder()
								.targetKey("color")
								.actionType(ActionType.SET)
								.modificationValue("red")
								.build(),
						TransformationRule.builder()
								.targetKey("title")
								.actionType(ActionType.APPEND)
								.modificationValue(" (Mobile)")
								.build()
				))
				.build();

		filter = new ChannelCustomizationFilter();
		filter.setProfileRegistry(Map.of("mobile-app", mobileProfile));
	}

	@Test
	void applyCustomization_shouldMutateComponentState_whenChannelMatches() {
		ContentComponent component = ContentComponent.builder()
				.componentId("/test")
				.type("text/base")
				.state(Map.of("title", "Hello World"))
				.build();

		UserContext context = new UserContext(Map.of(ContextKey.CHANNEL_ID, "mobile-app"));

		StepVerifier.create(filter.applyCustomization(component, context))
				.expectNextMatches(result ->
						"red".equals(result.state().get("color")) &&
								"Hello World (Mobile)".equals(result.state().get("title"))
				)
				.verifyComplete();
	}

	@ParameterizedTest
	@CsvSource({
			"unknown-channel",
			"desktop-web"
	})
	void applyCustomization_shouldNotMutate_whenChannelMismatchOrUnknown(String channelId) {
		ContentComponent component = ContentComponent.builder()
				.componentId("/test")
				.type("text/base")
				.state(Map.of("title", "Hello World"))
				.build();

		UserContext context = new UserContext(Map.of(ContextKey.CHANNEL_ID, channelId));

		StepVerifier.create(filter.applyCustomization(component, context))
				.expectNextMatches(result ->
						result.state().get("color") == null &&
								"Hello World".equals(result.state().get("title"))
				)
				.verifyComplete();
	}
}
