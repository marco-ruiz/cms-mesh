package net.bopsys.cms.mesh.engine;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayout;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.SlotGroup;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates cache extraction, storage, and selective invalidation.
 *
 * @author Marco Ruiz
 */
class LayoutCacheTest {

	private LayoutCache layoutCache;

	@BeforeEach
	void setUp() {
		layoutCache = new LayoutCache(new net.bopsys.cms.mesh.config.MeshProperties.CacheConfig(true, 50000, 15));
	}

	@Test
	void testPutAndGetFromCache() {
		String cacheKey = "cache_key_1";
		PageLayoutDefinition def = PageLayoutDefinition.builder()
				.pagePath("/some/path")
				.templateName("template-1")
				.slots(Map.of())
				.build();
		SlotGroupDefinition slot1Def = new SlotGroupDefinition("slot1", List.of("/comp/1"));
		List<SlotGroup> slots = List.of(new SlotGroup(slot1Def, List.of(
				ContentComponent.builder()
						.componentId("/comp/1")
						.type("test")
						.state(Map.of())
						.build()
		)));
		PageLayout layout = new PageLayout(def, slots);

		layoutCache.put(cacheKey, layout);

		PageLayout cached = layoutCache.get(cacheKey);

		assertThat(cached).isNotNull();
		assertThat(cached.slots().get("slot1").components()).hasSize(1);
	}

	@ParameterizedTest
	@CsvSource({
			"page_tag, /test/page",
			"slot_tag, /test/comp1",
			"multiple_tags, /test/page"
	})
	void testInvalidateByTags(String scenario, String tagToInvalidate) {
		String cacheKey = "key_" + scenario;
		SlotGroupDefinition slotDef = new SlotGroupDefinition(
				"testSlot",
				List.of("/test/comp1", "/test/comp2")
		);
		PageLayoutDefinition def = PageLayoutDefinition.builder()
				.pagePath("/test/page")
				.templateName("tpl")
				.slots(Map.of("testSlot", slotDef))
				.build();
		PageLayout layout = PageLayout.builder()
				.definition(def)
				.slots(Map.of())
				.build();

		layoutCache.put(cacheKey, layout);

		assertThat(layoutCache.get(cacheKey)).isNotNull();

		StepVerifier.create(layoutCache.invalidateReactive(List.of(tagToInvalidate)))
				.verifyComplete();

		assertThat(layoutCache.get(cacheKey)).isNull();
	}

	@Test
	void testInvalidateUnknownTagDoesNotAffectCache() {
		String cacheKey = "key_unknown";
		PageLayoutDefinition def = PageLayoutDefinition.builder()
				.pagePath("/test/page")
				.templateName("tpl")
				.slots(Map.of())
				.build();
		PageLayout layout = PageLayout.builder()
				.definition(def)
				.slots(Map.of())
				.build();

		layoutCache.put(cacheKey, layout);

		StepVerifier.create(layoutCache.invalidateReactive(List.of("/unknown/tag")))
				.verifyComplete();

		assertThat(layoutCache.get(cacheKey)).isNotNull();
	}
}
