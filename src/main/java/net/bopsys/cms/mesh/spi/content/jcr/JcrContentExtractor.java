package net.bopsys.cms.mesh.spi.content.jcr;

import net.bopsys.cms.mesh.config.JcrProperties;
import net.bopsys.cms.mesh.model.CmsConstants;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import lombok.RequiredArgsConstructor;

import static net.bopsys.cms.mesh.util.UnsafeFunction.extractValue;

/**
 * Handles JCR node transformations mapping to matching domain structures.
 *
 * @author Marco Ruiz
 */
@Component
@RequiredArgsConstructor
public class JcrContentExtractor {

	@SuppressWarnings("unchecked")
	private static <T> Stream<T> toStream(Iterator<?> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
				.map(obj -> (T) obj);
	}

	private static String getPropertyValue(Node node, String prop) throws RepositoryException {
		return getPropertyValue(node, prop, null);
	}

	private static String getPropertyValue(Node node, String prop, String defaultValue) throws RepositoryException {
		return (prop != null && node.hasProperty(prop)) ? node.getProperty(prop).getString() : defaultValue;
	}

	private static final Set<String> JCR_PREFIXES = Set.of("jcr:", "nt:", "mix:");

	private final JcrProperties jcrProps;

	/**
	 * Translates coordinates to layout structures.
	 *
	 * @param pagePath page identification path
	 * @param pageNode JCR source page node
	 * @return deserialized PageLayoutDefinition structure
	 * @throws RepositoryException on JCR storage failures
	 */
	public PageLayoutDefinition parsePageLayout(String pagePath, Node pageNode) throws RepositoryException {
		List<SlotGroupDefinition> slotDefs = pageNode.hasNode(CmsConstants.SLOT_JCR_CONTENT) ?
				JcrContentExtractor.<Node>toStream(pageNode.getNode(CmsConstants.SLOT_JCR_CONTENT).getNodes())
				.map(slotNode -> extractValue(slotNode, this::createSlotGroupDefinition, "Failed to wrap slot node"))
				.filter(s -> !s.componentIds().isEmpty())
				.toList() :
				List.of();

		return new PageLayoutDefinition(pagePath, extractTemplate(pageNode), slotDefs);
	}

	private SlotGroupDefinition createSlotGroupDefinition(Node node) throws RepositoryException {
		return new SlotGroupDefinition(
				node.getName(),
				JcrContentExtractor.<Node>toStream(node.getNodes())
						.map(n -> extractValue(n, Node::getPath, "Failed to get path"))
						.toList()
		);
	}

	private String extractTemplate(Node pageNode) throws RepositoryException {
		String slingTpl = getPropertyValue(pageNode, jcrProps.getSlingTemplateProperty());
		return slingTpl != null ? slingTpl : Objects.toString(
				getPropertyValue(pageNode, jcrProps.getCqTemplateProperty()),
				CmsConstants.DEFAULT_TEMPLATE
		);
	}

	/**
	 * Translates components values mapping to matching domain records.
	 *
	 * @param path component identity path
	 * @param node JCR component node source
	 * @return component configuration structure
	 * @throws RepositoryException on JCR storage failures
	 */
	public ContentComponent parseComponent(String path, Node node) throws RepositoryException {
		return ContentComponent.builder()
				.componentId(path)
				.type(getPropertyValue(node, jcrProps.getSlingResourceTypeProperty(),
						node.getPrimaryNodeType().getName()))
				.state(extractProperties(node))
				.resourceSuperType(getPropertyValue(node, jcrProps.getSlingResourceSuperTypeProperty()))
				.build();
	}

	private Map<String, Object> extractProperties(Node node) throws RepositoryException {
		Map<String, Object> properties = new HashMap<>();

		PropertyIterator it = node.getProperties();
		while (it.hasNext()) {
			Property prop = it.nextProperty();
			String name = prop.getName();

			if (JCR_PREFIXES.stream().noneMatch(name::startsWith))
				properties.put(name, extractPropValue(prop));
		}

		return properties;
	}

	private Object extractPropValue(Property prop) throws RepositoryException {
		return prop.isMultiple() ?
				Arrays.stream(prop.getValues()).map(this::extractString).toList() :
				extractString(prop.getValue());
	}

	private String extractString(Value val) {
		return extractValue(val, Value::getString, "Failed to read value string");
	}
}
