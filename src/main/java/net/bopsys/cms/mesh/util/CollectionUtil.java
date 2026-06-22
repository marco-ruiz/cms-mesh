package net.bopsys.cms.mesh.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility for collection conversions and safe manipulations.
 *
 * @author Marco Ruiz
 */
public final class CollectionUtil {

	private CollectionUtil() {}

	/**
	 * Safely converts a list of items to an order-preserving Map.
	 *
	 * @param list      the source list
	 * @param keyMapper function to extract keys from items
	 * @param <K>       the key type
	 * @param <V>       the value type
	 * @return An ordered map wrapping the items using the provided keys extractor.
	 */
	public static <K, V> Map<K, V> toOrderedMap(List<V> list, Function<V, K> keyMapper) {
		return (list == null) ?
				Map.of() :
				list.stream()
						.filter(Objects::nonNull)
						.collect(Collectors.toMap(
								keyMapper,
								v -> v,
								(existing, replacement) -> replacement,
								LinkedHashMap::new
						));
	}
}
