package net.bopsys.cms.mesh.model;

import lombok.Builder;
import java.util.Map;

/**
 * Record representing a hierarchical component configuration analogous to a JCR node.
 * 
 * @param componentId Unique identifier or path of the component
 * @param type The functional type describing its frontend equivalent (e.g. "commerce/banner")
 * @param state Flexible, unstructured key-value state mappings for the component properties
 * @param resourceSuperType Optional inheritance path pointing to a parent component configuration
 *
 * @author Marco Ruiz
 */
@Builder(toBuilder = true)
public record ContentComponent(
    String componentId,
    String type,
    Map<String, Object> state,
    String resourceSuperType
) {

	public ContentComponent buildCopyWithState(Map<String, Object> state) {
		return toBuilder().state(state).build();
	}
}
