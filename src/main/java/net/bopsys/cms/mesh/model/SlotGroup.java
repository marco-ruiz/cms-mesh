package net.bopsys.cms.mesh.model;

import net.bopsys.cms.mesh.util.CollectionUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents fully resolved runtime slots containing domain payload elements.
 *
 * @author Marco Ruiz
 */
public record SlotGroup(SlotGroupDefinition definition, Map<String, ContentComponent> components) {
	
	public SlotGroup(SlotGroupDefinition definition, List<ContentComponent> componentsList) {
		this(definition, CollectionUtil.toOrderedMap(componentsList, ContentComponent::componentId));
	}

	public String slotName() {
		return definition.slotName();
	}

	public Stream<SlottedComponent> slottedComponentsStream() {
		return components.values().stream().map(comp -> new SlottedComponent(slotName(), comp));
	}
}
