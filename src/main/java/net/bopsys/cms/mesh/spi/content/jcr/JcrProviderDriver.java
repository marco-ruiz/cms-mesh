package net.bopsys.cms.mesh.spi.content.jcr;

import lombok.RequiredArgsConstructor;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import net.bopsys.cms.mesh.util.ProviderDriverFallbackProvider;
import net.bopsys.cms.mesh.util.ReactiveExecutionUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver for a JCR content provider. Offloads blocking JCR network APIs to the reactive bounded elastic pool.
 *
 * @author Marco Ruiz
 */
@Component
@RequiredArgsConstructor
public class JcrProviderDriver implements ProviderDriver {

	private final JcrSessionManager sessionManager;
	private final JcrContentExtractor contentExtractor;

	@Override
	public ProviderDriverProtocol getSupportedProtocol() {
		return ProviderDriverProtocol.LOCAL_JCR;
	}

	@Override
	public Mono<PageLayoutDefinition> resolvePageLayout(String pagePath) {
		return ReactiveExecutionUtil.executeWithFallback(
				() -> sessionManager.execute(session ->
						contentExtractor.parsePageLayout(pagePath, session.getNode(pagePath))),
				RepositoryException.class,
				e -> ProviderDriverFallbackProvider.fallbackLayoutDefinition(pagePath, e)
		);
	}

	@Override
	public Mono<ContentComponent> resolveComponent(String componentPath) {
		return ReactiveExecutionUtil.executeWithFallback(
				() -> sessionManager.execute(session ->
						contentExtractor.parseComponent(componentPath, session.getNode(componentPath))),
				RepositoryException.class,
				e -> ProviderDriverFallbackProvider.fallbackComponent(componentPath, e)
		).flatMap(this::resolveInheritance);
	}

	private Mono<ContentComponent> resolveInheritance(ContentComponent node) {
		return node.resourceSuperType() != null ?
				resolveComponent(node.resourceSuperType()).map(parent -> mergeComponents(parent, node)) :
				Mono.just(node);
	}

	private ContentComponent mergeComponents(ContentComponent parentComp, ContentComponent node) {
		Map<String, Object> mergedState = new HashMap<>(parentComp.state());
		mergedState.putAll(node.state());

		return ContentComponent.builder()
				.componentId(node.componentId())
				.type(node.type())
				.state(mergedState)
				.resourceSuperType(node.resourceSuperType())
				.build();
	}
}
