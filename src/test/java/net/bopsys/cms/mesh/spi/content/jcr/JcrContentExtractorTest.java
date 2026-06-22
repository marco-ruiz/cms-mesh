package net.bopsys.cms.mesh.spi.content.jcr;

import net.bopsys.cms.mesh.config.JcrProperties;
import net.bopsys.cms.mesh.model.ContentComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JcrContentExtractor mapping raw JCR structures to ContentComponent.
 *
 * @author Marco Ruiz
 */
class JcrContentExtractorTest {

	private JcrContentExtractor extractor;
	private JcrProperties properties;

	@BeforeEach
	void setUp() {
		properties = new JcrProperties(
				null, null, null, null, null, null, "cq:template", "cq:template",
				"sling:resourceType", "sling:resourceSuperType"
		);
		extractor = new JcrContentExtractor(properties);
	}

	@ParameterizedTest(name = "{index} => Component Id: {0}, Resource Type: {1}, Super Type: {2}")
	@MethodSource("componentNodeProvider")
	@DisplayName("Parametrized extraction of JCR components mapping to ContentComponent")
	void testParseComponent(
			String expectedId,
			String expectedType,
			String expectedSuperType) throws Exception {

		Node mockNode = expectedType.equals("nt:unstructured") ?
				createStubNode(expectedType, null) :
				createStubNode(expectedType, expectedSuperType);

		ContentComponent result = extractor.parseComponent(expectedId, mockNode);

		assertThat(result).isNotNull();
		assertThat(result.componentId()).isEqualTo(expectedId);
		assertThat(result.type()).isEqualTo(expectedType);
		assertThat(result.resourceSuperType()).isEqualTo(expectedSuperType);
	}

	private static Stream<Arguments> componentNodeProvider() {
		return Stream.of(
				Arguments.of("/content/my-site/comp1", "my-app/components/title", "my-app/components/base"),
				Arguments.of("/content/my-site/comp2", "my-app/components/text", null),
				Arguments.of("/content/my-site/comp3", "nt:unstructured", null)
		);
	}

	private static <T> T createProxy(Class<T> interfaceClass, java.lang.reflect.InvocationHandler handler) {
		return interfaceClass.cast(Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class<?>[]{interfaceClass},
				handler
		));
	}

	private static Node createStubNode(String type, String superType) {
		return createProxy(Node.class, (proxy, method, args) -> handleNodeMethod(method.getName(), args, type, superType));
	}

	private static Object handleNodeMethod(String methodName, Object[] args, String type, String superType) {
		Map<String, java.util.function.Supplier<Object>> handlers = Map.of(
				"hasProperty", () -> hasPropertyStub((String) args[0], type, superType),
				"getProperty", () -> getPropertyStub((String) args[0], type, superType),
				"getPrimaryNodeType", () -> createStubNodeType(type),
				"getProperties", JcrContentExtractorTest::createEmptyPropertyIterator
		);
		return handlers.containsKey(methodName) ? handlers.get(methodName).get() : null;
	}

	private static Boolean hasPropertyStub(String arg, String type, String superType) {
		return "sling:resourceType".equals(arg) ?
				!type.equals("nt:unstructured") :
				"sling:resourceSuperType".equals(arg) ?
						superType != null :
						false;
	}

	private static Property getPropertyStub(String arg, String type, String superType) {
		return "sling:resourceType".equals(arg) ?
				createStubProperty(type) :
				"sling:resourceSuperType".equals(arg) ?
						createStubProperty(superType) :
						null;
	}

	private static Property createStubProperty(String value) {
		return createProxy(Property.class, (proxy, method, args) ->
				"getString".equals(method.getName()) ? value :
				"isMultiple".equals(method.getName()) ? false :
				null
		);
	}

	private static NodeType createStubNodeType(String name) {
		return createProxy(NodeType.class, (proxy, method, args) -> "getName".equals(method.getName()) ? name : null);
	}

	private static PropertyIterator createEmptyPropertyIterator() {
		return createProxy(PropertyIterator.class, (proxy, method, args) -> "hasNext".equals(method.getName()) ? false : null);
	}
}
