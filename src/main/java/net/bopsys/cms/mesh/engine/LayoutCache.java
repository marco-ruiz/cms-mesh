package net.bopsys.cms.mesh.engine;

import lombok.Getter;
import net.bopsys.cms.mesh.cache.BiDirectionalIndexCache;
import net.bopsys.cms.mesh.config.MeshProperties;
import net.bopsys.cms.mesh.model.PageLayout;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Coordinated cache index manager mapping page layouts to calculated component structures.
 *
 * @author Marco Ruiz
 */
public class LayoutCache extends BiDirectionalIndexCache<String, PageLayout> {

	@Getter
	private final boolean enabled;

	/**
	 * Initializes the layouts cache index with eviction bounds.
	 *
	 * @param cacheConfig dynamic cache configurations
	 */
	public LayoutCache(MeshProperties.CacheConfig cacheConfig) {
		super(
				cacheConfig == null ? 50000 : cacheConfig.maxSize(),
				cacheConfig == null ? 15 : cacheConfig.expireAfterWriteMinutes()
		);
		this.enabled = cacheConfig != null && cacheConfig.enabled();
	}

	@Override
	public PageLayout get(String key) {
		return enabled ? super.get(key) : null;
	}

	/**
	 * Reactor friendly fetch wrapper encapsulating cache checking.
	 *
	 * @param key           cache lookup key
	 * @param valueSupplier fallback value creator
	 * @return reactive layout producer
	 */
	public Mono<PageLayout> getReactive(String key, Supplier<Mono<PageLayout>> valueSupplier) {
		return super.getReactive(key, valueSupplier, enabled);
	}

	@Override
	public void put(String key, Set<String> tags, PageLayout value) {
		if (enabled)
			super.put(key, tags, value);
	}

	/**
	 * Maps page coordinates against extracted content signatures.
	 *
	 * @param cacheKey layout lookups entry key
	 * @param layout   layout spec details
	 */
	public void put(String cacheKey, PageLayout layout) {
		if (!enabled)
			return;

		Set<String> tags = (layout != null && layout.definition() != null) ?
				layout.definition().getTags() :
				new HashSet<>();
		put(cacheKey, tags, layout);
	}

	/**
	 * Standard reactive invalidation handler executing sequential tag updates.
	 *
	 * @param tags dynamic target collection
	 * @return completion signal
	 */
	public Mono<Void> invalidateReactive(List<String> tags) {
		return Mono.fromRunnable(() -> super.invalidate(tags));
	}
}
