package net.bopsys.cms.mesh.model;

import java.util.List;

/**
 * Represents unresolved schema blueprints containing raw pointer arrays.
 *
 * @author Marco Ruiz
 */
public record SlotGroupDefinition(String slotName, List<String> componentIds) {
	
	public SlotGroupDefinition(String slotName, List<String> componentIds) {
		this.slotName = slotName;
		this.componentIds = componentIds == null ? List.of() : List.copyOf(componentIds);
	}
}
