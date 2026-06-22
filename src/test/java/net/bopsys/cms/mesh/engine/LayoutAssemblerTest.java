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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Validates layout assembler configuration orchestration.
 *
 * @author Marco Ruiz
 */
class LayoutAssemblerTest {

	private ProviderDriver providerMock;
	private CustomizationFilter customizationMock;
	private LayoutAssembler assembler;

	private int resolvePageLayoutCalls = 0;

	@BeforeEach
	void setUp() {
		resolvePageLayoutCalls = 0;

		providerMock = new ProviderDriver() {
			@Override
			public ProviderDriverProtocol getSupportedProtocol() {
				return ProviderDriverProtocol.LOCAL_JCR;
			}

			@Override
			public Mono<PageLayoutDefinition> resolvePageLayout(String pagePath) {
				resolvePageLayoutCalls++;
				PageLayoutDefinition layout = PageLayoutDefinition.builder()
						.pagePath(pagePath)
						.templateName("test-template")
						.slots(Map.of(
								"header", new SlotGroupDefinition("header", List.of("/comp/1")),
								"hero", new SlotGroupDefinition("hero", List.of("/comp/hero_banner")),
								"items", new SlotGroupDefinition("items", List.of("/comp/product_list"))
						))
						.build();
				return Mono.just(layout);
			}

			@Override
			public Mono<ContentComponent> resolveComponent(String componentPath) {
				ContentComponent comp = ContentComponent.builder()
						.componentId(componentPath)
						.type("test/type")
						.state(Map.of("prop", "val"))
						.build();
				return Mono.just(comp);
			}
		};

		customizationMock = (incoming, userContext) -> Mono.just(incoming);

		CacheConfig cacheConfig = new CacheConfig(true, 50000, 15);
		MeshProperties props = new MeshProperties(ProviderDriverProtocol.LOCAL_JCR, cacheConfig);
		ProviderDriversRegistry registry = new ProviderDriversRegistry(props.defaultProviderDriver(), List.of(providerMock));
		LayoutCache cache = new LayoutCache(props.cache());
		CustomizationFiltersRegistry filtersRegistry = new CustomizationFiltersRegistry(List.of(customizationMock));
		assembler = new LayoutAssembler(registry, filtersRegistry, cache);
	}

	@ParameterizedTest
	@CsvSource({
			"/mock/page, header, /comp/1",
			"/content/home, hero, /comp/hero_banner",
			"/products, items, /comp/product_list"
	})
	void assemblePageLayout_shouldResolveSlotsAndComponents(String pagePath, String slotName, String componentId) {
		UserContext ctx = new UserContext(Map.of());

		StepVerifier.create(assembler.assemblePageLayout(pagePath, ctx, ProviderDriverProtocol.LOCAL_JCR))
				.expectNextMatches(result ->
						result.slots().containsKey(slotName) &&
								result.slots().get(slotName).components().size() == 1 &&
								componentId.equals(result.slots().get(slotName).components().get(componentId).componentId())
				)
				.verifyComplete();

		StepVerifier.create(assembler.assemblePageLayout(pagePath, ctx, ProviderDriverProtocol.LOCAL_JCR))
				.expectNextCount(1)
				.verifyComplete();

		Assertions.assertEquals(1, resolvePageLayoutCalls, "resolvePageLayoutCalls should be 1");
	}

	@Test
	void assemblePageLayout_shouldMissCacheForDifferentContext() {
		String pagePath = "/mock/page";

		UserContext ctx1 = new UserContext(Map.of(ContextKey.TIER, "VIP"));
		UserContext ctx2 = new UserContext(Map.of(ContextKey.TIER, "REGULAR"));

		StepVerifier.create(assembler.assemblePageLayout(pagePath, ctx1, ProviderDriverProtocol.LOCAL_JCR))
				.expectNextCount(1)
				.verifyComplete();

		StepVerifier.create(assembler.assemblePageLayout(pagePath, ctx2, ProviderDriverProtocol.LOCAL_JCR))
				.expectNextCount(1)
				.verifyComplete();

		Assertions.assertEquals(2, resolvePageLayoutCalls);
	}

	@ParameterizedTest
	@MethodSource("provideTestMatrix")
	void assemblePageLayout_shouldProcessMatrixCombinations(
			String pagePath, String tier, String channelId, ProviderDriverProtocol providerProtocol) {

		Map<ContextKey, String> attrs = new HashMap<>();
		if (tier != null)
			attrs.put(ContextKey.TIER, tier);
		if (channelId != null)
			attrs.put(ContextKey.CHANNEL_ID, channelId);

		UserContext ctx = new UserContext(attrs);

		StepVerifier.create(assembler.assemblePageLayout(pagePath, ctx, providerProtocol))
				.expectNextMatches(result -> result != null && !result.slots().isEmpty())
				.verifyComplete();
	}

	static Stream<Arguments> provideTestMatrix() {
		return Stream.of(
				Arguments.of("/content/home", "VIP", "mobile-app", ProviderDriverProtocol.LOCAL_JCR),
				Arguments.of("/content/home", "REGULAR", "desktop-web", ProviderDriverProtocol.LOCAL_JCR),
				Arguments.of("/content/products", "VIP", "mobile-app", ProviderDriverProtocol.LOCAL_JCR),
				Arguments.of("/content/products", "REGULAR", "desktop-web", ProviderDriverProtocol.LOCAL_JCR),
				Arguments.of("/content/edge-case", null, null, ProviderDriverProtocol.LOCAL_JCR)
		);
	}
}
