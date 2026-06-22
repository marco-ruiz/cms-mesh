package net.bopsys.cms.mesh.spi.content.aem;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import net.bopsys.cms.mesh.config.AemProperties;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Driver for an AEM content provider.
 *
 * @author Marco Ruiz
 */
@Component
@Slf4j
public class AemProviderDriver implements ProviderDriver {

	private final AemProperties aemProperties;
	private final WebClient webClient;
	private final AemContentExtractor contentExtractor;

	public AemProviderDriver(
			AemProperties aemProperties, WebClient.Builder webClientBuilder, AemContentExtractor contentExtractor) {
		this.aemProperties = aemProperties;
		this.webClient = webClientBuilder.build();
		this.contentExtractor = contentExtractor;
	}

	@Override
	public ProviderDriverProtocol getSupportedProtocol() {
		return ProviderDriverProtocol.AEM_GRAPHQL;
	}

	@Override
	public Mono<ContentComponent> resolveComponent(String componentPath) {
		log.debug("Resolving AEM component via persisted query for path: {}", componentPath);

		return this.webClient.get()
				.uri(buildPersistedQueryUri(componentPath))
				.header(HttpHeaders.AUTHORIZATION, getAuthHeader())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.flatMap(json -> validateAndExtractItem(json, componentPath))
				.map(itemNode -> contentExtractor.parse(itemNode, componentPath))
				.onErrorResume(e -> handleFailure(componentPath, e));
	}

	private String buildPersistedQueryUri(String componentPath) {
		// Fallback prefix if no endpoint is configured in your application properties
		String baseEndpoint = aemProperties.graphqlEndpoint() != null
				? aemProperties.graphqlEndpoint()
				: "/graphql/execute.json/my-project";

		// AEM expects variables appended as a semicolon-separated path parameter for GET requests
		return baseEndpoint + "/component-by-path;path=" + componentPath;
	}

	@Override
	public Mono<PageLayoutDefinition> resolvePageLayout(String pagePath) {
		log.warn("AEM native layout schema tracking not implemented. Path '{}' bypassed to structural fallback layers.", pagePath);
		return Mono.empty();
	}

	private String getAuthHeader() {
		return "Bearer " + aemProperties.accessToken();
	}

	private Mono<JsonNode> validateAndExtractItem(JsonNode json, String componentPath) {
		if (json.has("errors") && !json.get("errors").isEmpty()) {
			log.warn("AEM GraphQL server side errors caught for path {}: {}", componentPath, json.get("errors"));
			return Mono.empty();
		}

		JsonNode itemNode = json.at("/data/componentByPath/item");
		if (itemNode.isMissingNode() || itemNode.isNull()) {
			log.debug("No AEM component layout metadata resolved for path: {}", componentPath);
			return Mono.empty();
		}

		return Mono.just(itemNode);
	}

	private Mono<ContentComponent> handleFailure(String componentPath, Throwable e) {
		log.error("Fatal I/O connection failure while invoking AEM GraphQL service for path: {}", componentPath, e);
		return Mono.empty();
	}
}