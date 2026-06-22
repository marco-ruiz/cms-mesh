package net.bopsys.cms.mesh.model;

import net.bopsys.cms.mesh.util.CollectionUtil;

import lombok.Builder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Root delivery and cache payload matrix.
 *
 * @author Marco Ruiz
 */
@Builder
public record PageLayout(PageLayoutDefinition definition, Map<String, SlotGroup> slots) {
    
	public PageLayout(PageLayoutDefinition definition, List<SlotGroup> slotsList) {
		this(definition, CollectionUtil.toOrderedMap(slotsList, SlotGroup::slotName));
	}

	public List<SlottedComponent> getSlottedComponents() {
		return slots().values().stream().flatMap(SlotGroup::slottedComponentsStream).toList();
	}
}
