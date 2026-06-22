package net.bopsys.cms.mesh.model;

import net.bopsys.cms.mesh.util.CollectionUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Builder;

/**
 * Record representing a conceptual page and its logical slots pointing to component IDs.
 *
 * @param pagePath The URI path for the resolving page
 * @param templateName The rendering template assigned to this layout
 * @param slots Map of logical slot names (e.g. "header", "main") to a list of Component IDs
 */
@Builder
public record PageLayoutDefinition(String pagePath, String templateName, Map<String, SlotGroupDefinition> slots) {

	public PageLayoutDefinition(String pagePath, String templateName, List<SlotGroupDefinition> slotsList) {
		this(pagePath, templateName, CollectionUtil.toOrderedMap(slotsList, SlotGroupDefinition::slotName));
	}

	/**
	 * Retrieves the collection of slot definitions.
	 *
	 * @return collection of slot definitions
	 */
	public Collection<SlotGroupDefinition> getSlotDefinitions() {
		return slots != null ? slots.values() : Collections.emptyList();
	}

	/**
	 * Extracts dynamic cache invalidation tags representing page and component ids.
	 *
	 * @return target tags collection
	 */
	public Set<String> getTags() {
		Set<String> tags = new HashSet<>();
		if (pagePath != null)
			tags.add(pagePath);

		if (slots != null)
			slots.values().stream()
					.filter(Objects::nonNull)
					.map(SlotGroupDefinition::componentIds)
					.filter(Objects::nonNull)
					.forEach(tags::addAll);

		return tags;
	}
}
