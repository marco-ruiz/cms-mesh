package net.bopsys.cms.mesh.engine;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayout;
import net.bopsys.cms.mesh.model.SlotGroup;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;
import net.bopsys.cms.mesh.model.SlottedComponent;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.ProviderDriver;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/**
 * Platform execution pipeline resolving structures from targeted content providers.
 *
 * @author Marco Ruiz
 */
public class LayoutAssembler {

	private final ProviderDriversRegistry providerRegistry;
	private final CustomizationFiltersRegistry customizationFiltersRegistry;
	private final LayoutCache layoutCache;

	/**
	 * Instantiates LayoutAssembler using custom registry, filters, and cache.
	 *
	 * @param providers registry managing layout content providers
	 * @param filters   ordered transformations pipeline
	 * @param cache     layouts cache manager
	 */
	public LayoutAssembler(ProviderDriversRegistry providers, CustomizationFiltersRegistry filters, LayoutCache cache) {
		this.providerRegistry = providers;
		this.customizationFiltersRegistry = filters;
		this.layoutCache = cache;
	}

	/**
	 * Streams individual processed components.
	 *
	 * @param pagePath         targeted routing coordinate
	 * @param ctx          user credentials profile
	 * @param providerId target connector protocol
	 * @return layout stream emitting individual items
	 */
	public Flux<SlottedComponent> streamPageLayout(String pagePath, UserContext ctx, ProviderDriverProtocol providerId) {
		return assemblePageLayout(pagePath, ctx, providerId)
				.flatMapMany(layout -> Flux.fromIterable(layout.getSlottedComponents()));
	}

	/**
	 * Resolves page layout and inflates content slots.
	 *
	 * @param pagePath         the content route requested
	 * @param ctx          the calculated user scope profile parameters
	 * @param providerId the backend protocol identity
	 * @return map resolving slots to matching sorted arrays of target components
	 */
	public Mono<PageLayout> assemblePageLayout(String pagePath, UserContext ctx, ProviderDriverProtocol providerId) {
		return new AssemblyRequest(pagePath, ctx, providerRegistry.getProvider(providerId)).assemble();
	}

	/**
	 * Programmatic custom entries invalidator interface.
	 *
	 * @param tags target markers to wipe
	 * @return completion signal
	 */
	public Mono<Void> invalidateCache(List<String> tags) {
		return layoutCache.invalidateReactive(tags);
	}

	@AllArgsConstructor
	public class AssemblyRequest {
		private final String pagePath;
		private final UserContext ctx;
		private final ProviderDriver provider;

		public Mono<PageLayout> assemble() {
			String cacheKey = generateCacheKey();
			return layoutCache.getReactive(cacheKey, () -> buildAndCacheLayout(cacheKey));
		}

		private String generateCacheKey() {
			String contextStr = ctx != null ? ctx.fingerprint() : "";
			String providerName = (provider != null && provider.getSupportedProtocol() != null) ?
					provider.getSupportedProtocol().name() : "DEFAULT";

			return String.join("|", pagePath, contextStr, providerName);
		}

		private Mono<PageLayout> buildAndCacheLayout(String cacheKey) {
			return provider.resolvePageLayout(pagePath)
					.flatMap(this::createPageLayout)
					.doOnNext(layout -> layoutCache.put(cacheKey, layout));
		}

		private Mono<PageLayout> createPageLayout(PageLayoutDefinition pageLayoutDef) {
			return Flux.fromIterable(pageLayoutDef.getSlotDefinitions())
					.flatMapSequential(this::createSlotGroup)
					.collectList()
					.map(groups -> new PageLayout(pageLayoutDef, groups));
		}

		private Mono<SlotGroup> createSlotGroup(SlotGroupDefinition slotDef) {
			return Flux.fromIterable(slotDef.componentIds())
					.flatMapSequential(this::fetchAndCustomize)
					.collectList()
					.map(components -> new SlotGroup(slotDef, components));
		}

		private Mono<ContentComponent> fetchAndCustomize(String id) {
			return provider.resolveComponent(id)
					.flatMap(component -> customizationFiltersRegistry.applyFilters(component, ctx));
		}
	}
}
