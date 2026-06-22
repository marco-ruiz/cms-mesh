package net.bopsys.cms.mesh.spi.content.cmis;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Property;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Handles mapping of raw CMIS compliant repositories nodes to domain models.
 *
 * @author Marco Ruiz
 */
@Component
public class CmisContentExtractor {

	private static final String PROPERTY_CMIS_PATH = "cmis:path";

	/**
	 * Resolves folder attributes mapping slots properties.
	 *
	 * @param pagePath     targeted page URL route
	 * @param rootObject   raw folder node from repo
	 * @param templateName custom layout template
	 * @return deserialized page layout representation
	 */
	public PageLayoutDefinition parsePageLayout(String pagePath, CmisObject rootObject, String templateName) {
		List<SlotGroupDefinition> slotDefs = (rootObject instanceof Folder folder) ?
				StreamSupport.stream(folder.getChildren().spliterator(), false)
				.filter(Folder.class::isInstance)
				.map(Folder.class::cast)
				.map(this::getSlotGroupDefinition)
				.toList() :
				List.of();
		return new PageLayoutDefinition(pagePath, templateName, slotDefs);
	}

	private SlotGroupDefinition getSlotGroupDefinition(Folder slotFolder) {
		return new SlotGroupDefinition(
				slotFolder.getName(),
				StreamSupport.stream(slotFolder.getChildren().spliterator(), false)
						.map(c -> (String) c.getPropertyValue(PROPERTY_CMIS_PATH))
						.toList()
		);
	}

	/**
	 * Extracts component property map and translates it to domain structures.
	 *
	 * @param path       target component URL route
	 * @param cmisObject raw source object
	 * @return component configuration construct
	 */
	public ContentComponent parseComponent(String path, CmisObject cmisObject) {
		return ContentComponent.builder()
				.componentId(path)
				.type(cmisObject.getBaseTypeId().value())
				.state(extractProperties(cmisObject))
				.build();
	}

	private Map<String, Object> extractProperties(CmisObject cmisObj) {
		return cmisObj.getProperties().stream()
				.collect(Collectors.toMap(Property::getId, this::extractPropertyValue));
	}

	private Object extractPropertyValue(Property<?> property) {
		return property.isMultiValued() ? property.getValues() : property.getValue();
	}
}
