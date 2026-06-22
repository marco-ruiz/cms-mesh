package net.bopsys.cms.mesh.engine;

import net.bopsys.cms.mesh.config.MeshProperties;
import net.bopsys.cms.mesh.config.MeshProperties.CacheConfig;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import net.bopsys.cms.mesh.spi.CustomizationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parameterized unit tests for LayoutAssembler validating diverse user contexts.
 *
 * @author Marco Ruiz
 */
class LayoutAssemblerParameterizedTest {

	private ProviderDriver providerMock;
	private CustomizationFilter customizationMock;
	private LayoutAssembler assembler;

	@BeforeEach
	void setUp() {
		providerMock = new ProviderDriver() {
			@Override
			public ProviderDriverProtocol getSupportedProtocol() {
				return ProviderDriverProtocol.LOCAL_JCR;
			}

			@Override
			public Mono<PageLayoutDefinition> resolvePageLayout(String path) {
				return Mono.just(PageLayoutDefinition.builder()
						.pagePath(path)
						.templateName("template")
						.slots(Map.of("content", new SlotGroupDefinition("content", List.of("/comp/1"))))
						.build());
			}

			@Override
			public Mono<ContentComponent> resolveComponent(String path) {
				return Mono.just(ContentComponent.builder()
						.componentId(path)
						.type("test")
						.state(Map.of("prop", "val"))
						.build());
			}
		};

		customizationMock = (c, ctx) -> Mono.just(c);

		MeshProperties props = new MeshProperties(
				ProviderDriverProtocol.LOCAL_JCR,
				new CacheConfig(true, 1000, 5)
		);
		ProviderDriversRegistry registry = new ProviderDriversRegistry(props.defaultProviderDriver(), List.of(providerMock));
		LayoutCache cache = new LayoutCache(props.cache());
		CustomizationFiltersRegistry filtersRegistry = new CustomizationFiltersRegistry(List.of(customizationMock));
		assembler = new LayoutAssembler(registry, filtersRegistry, cache);
	}

	@ParameterizedTest
	@MethodSource("provideUserContextCombinations")
	void assemblePageLayout_withVariousContexts_shouldResolve(UserContext context, String desc) {
		StepVerifier.create(assembler.assemblePageLayout("/home", context, ProviderDriverProtocol.LOCAL_JCR))
				.expectNextMatches(result -> result.slots().containsKey("content"))
				.verifyComplete();
	}

	private static Stream<Arguments> provideUserContextCombinations() {
		return Stream.of(
				Arguments.of(new UserContext(Map.of(ContextKey.TIER, "VIP", ContextKey.CHANNEL_ID, "mobile")),
						"VIP tier with mobile channel"),
				Arguments.of(new UserContext(Map.of(ContextKey.TIER, "REGULAR", ContextKey.CHANNEL_ID, "web")),
						"Regular tier with web channel"),
				Arguments.of(new UserContext(Map.of(ContextKey.CHANNEL_ID, "tablet")),
						"Missing tier with tablet channel"),
				Arguments.of(new UserContext(Map.of(ContextKey.TIER, "GUEST")),
						"Guest tier without channel"),
				Arguments.of(new UserContext(Map.of()),
						"Blank context keys"),
				Arguments.of(new UserContext(createNullChannelContext()),
						"Context with null-like empty values")
		);
	}

	private static Map<ContextKey, String> createNullChannelContext() {
		Map<ContextKey, String> attributes = new HashMap<>();
		attributes.put(ContextKey.CHANNEL_ID, "");
		attributes.put(ContextKey.TIER, "");
		return attributes;
	}
}
