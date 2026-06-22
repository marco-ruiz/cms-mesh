package net.bopsys.cms.mesh.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe bi-directional index cache with tag mapping tracking.
 *
 * @param <K> Key type
 * @param <V> Value type
 *
 * @author Marco Ruiz
 */
public class BiDirectionalIndexCache<K, V> {

	private final ConcurrentHashMap<String, Set<K>> tagToKeyRegistry = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Set<String>> keyToTagRegistry = new ConcurrentHashMap<>();
	private final Cache<K, V> cache;

	public BiDirectionalIndexCache(long maxSize, long expireAfterWriteMinutes) {
		this.cache = Caffeine.newBuilder()
				.maximumSize(maxSize)
				.expireAfterWrite(Duration.ofMinutes(expireAfterWriteMinutes))
				.evictionListener((K key, V val, RemovalCause cause) -> removeKeyFromRegistry(key))
				.build();
	}

	public V get(K key) {
		return (key == null) ? null : cache.getIfPresent(key);
	}

	public Mono<V> getReactive(K key, Supplier<Mono<V>> valueSupplier, boolean fromCache) {
		return fromCache ? Mono.justOrEmpty(get(key)).switchIfEmpty(Mono.defer(valueSupplier)) : valueSupplier.get();
	}

	public void put(K key, Set<String> tags, V value) {
		if (key == null || value == null) return;
		cache.put(key, value);
		if (tags != null && !tags.isEmpty()) {
			keyToTagRegistry.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).addAll(tags);
			for (String tag : tags)
				tagToKeyRegistry.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(key);
		}
	}

	public void invalidate(List<String> tags) {
		if (tags == null) return;
		for (String tag : tags) {
			Set<K> keys = tagToKeyRegistry.remove(tag);
			if (keys != null) {
				for (K cacheKey : keys) {
					cache.invalidate(cacheKey);
					removeMapRelation(keyToTagRegistry, cacheKey, tag);
				}
			}
		}
	}

	private void removeKeyFromRegistry(K cacheKey) {
		if (cacheKey == null) return;
		Set<String> associatedTags = keyToTagRegistry.remove(cacheKey);
		if (associatedTags != null)
			for (String tag : associatedTags)
				removeMapRelation(tagToKeyRegistry, tag, cacheKey);
	}

	private <M, S> void removeMapRelation(ConcurrentHashMap<M, Set<S>> map, M masterKey, S slaveValue) {
		map.computeIfPresent(masterKey, (key, activeSet) -> {
			activeSet.remove(slaveValue);
			return activeSet.isEmpty() ? null : activeSet;
		});
	}
}
