package net.bopsys.cms.mesh.spi.content.aem;

import com.fasterxml.jackson.databind.JsonNode;
import net.bopsys.cms.mesh.model.ContentComponent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Extracts AEM GraphQL JsonNode documents to Mesh Content Components.
 *
 * @author Marco Ruiz
 */
@Component
class AemContentExtractor {

	private static final String DEFAULT_AEM_TYPE = "aem/component";
	private static final Set<String> IGNORED_KEYS = Set.of(
			"_path",
			"sling:resourceType",
			"type",
			"sling:resourceSuperType"
	);

	public ContentComponent parse(JsonNode itemNode, String componentPath) {
		Map<String, Object> state = new HashMap<>();

		String type = getPropertyAsString(itemNode, "sling:resourceType", getPropertyAsString(itemNode, "type", DEFAULT_AEM_TYPE));
		String resourceSuperType = getPropertyAsString(itemNode, "sling:resourceSuperType", null);
		itemNode.fields().forEachRemaining(entry -> extractState(entry, state));

		return ContentComponent.builder()
				.componentId(componentPath)
				.type(type)
				.resourceSuperType(resourceSuperType)
				.state(state)
				.build();
	}

	private void extractState(Map.Entry<String, JsonNode> entry, Map<String, Object> state) {
		String key = entry.getKey();
		JsonNode node = entry.getValue();

		if (IGNORED_KEYS.contains(key))
			return;

		if ("_metadata".equals(key)) {
			extractMetadataStrings(node, state);
		} else {
			extractPrimitiveValue(key, node, state);
		}
	}

	private void extractPrimitiveValue(String key, JsonNode node, Map<String, Object> state) {
		switch (node.getNodeType()) {
			case STRING  -> state.put(key, node.asText());
			case BOOLEAN -> state.put(key, node.asBoolean());
			case NUMBER  -> state.put(key, node.numberValue());
			default      -> { /* Skip multi-nested complex fragments */ }
		}
	}

	private void extractMetadataStrings(JsonNode metadataNode, Map<String, Object> state) {
		JsonNode stringMetadata = metadataNode.at("/stringMetadata");
		if (stringMetadata.isArray()) {
			for (JsonNode metaItem : stringMetadata) {
				if (metaItem.has("name") && metaItem.has("value")) {
					state.put(metaItem.get("name").asText(), metaItem.get("value").asText());
				}
			}
		}
	}

	private String getPropertyAsString(JsonNode node, String fieldName, String defaultValue) {
		JsonNode valueNode = node.get(fieldName);
		return (valueNode != null && valueNode.isTextual()) ? valueNode.asText() : defaultValue;
	}
}