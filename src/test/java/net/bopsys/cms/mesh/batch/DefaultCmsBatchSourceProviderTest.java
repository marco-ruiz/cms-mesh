package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for DefaultCmsBatchSourceProvider configuration resolution.
 *
 * @author Marco Ruiz
 */
class DefaultCmsBatchSourceProviderTest {

	@ParameterizedTest(name = "{index} => Configured Paths: {0}, Protocol: {1}")
	@MethodSource("sourceConfigurationProvider")
	@DisplayName("Parameterized resolution of batch source properties and defaults")
	void testSourceProviderConfiguration(
			List<String> inputPaths,
			ProviderDriverProtocol inputProtocol,
			List<String> expectedPaths,
			ProviderDriverProtocol expectedProtocol) {

		// Construct properties with record constructor (triggering compact canonical constructor sanitization)
		CmsBatchProperties props = new CmsBatchProperties(
				true, 10, 4, 8, 3, 1000L, inputPaths, inputProtocol
		);

		DefaultCmsBatchSourceProvider provider = new DefaultCmsBatchSourceProvider(props);

		assertThat(provider.getTargetPaths()).isEqualTo(expectedPaths);
		assertThat(provider.getTargetProtocol()).isEqualTo(expectedProtocol);
	}

	private static Stream<Arguments> sourceConfigurationProvider() {
		return Stream.of(
				Arguments.of(
						List.of("/content/custom1", "/content/custom2"),
						ProviderDriverProtocol.AEM_GRAPHQL,
						List.of("/content/custom1", "/content/custom2"),
						ProviderDriverProtocol.AEM_GRAPHQL
				),
				Arguments.of(
						null, // Should fallback to default record initializer
						null,
						List.of("/content/home", "/content/products"),
						ProviderDriverProtocol.LOCAL_JCR
				)
		);
	}
}