package net.bopsys.cms.mesh.spi.content.cmis;

import net.bopsys.cms.mesh.config.CmisProperties;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import net.bopsys.cms.mesh.util.ProviderDriverFallbackProvider;
import net.bopsys.cms.mesh.util.ReactiveExecutionUtil;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Driver for a CMIS content provider. Resolves CMS configurations from remote CMIS compliant systems.
 *
 * @author Marco Ruiz
 */
@Component
public class CmisProviderDriver implements ProviderDriver {

	private final SessionFactory sessionFactory;
	private final CmisProperties cmisProperties;
	private final CmisContentExtractor contentExtractor;
	private final String templateName;
	private final Object sessionMonitor = new Object();
	private volatile Session cmisSession;

	public CmisProviderDriver(SessionFactory sessionFactory, CmisProperties props, CmisContentExtractor extractor) {
		this.sessionFactory = sessionFactory;
		this.cmisProperties = props;
		this.contentExtractor = extractor;
		this.templateName = props.getTemplateName();
	}

	private Session getSession() {
		if (cmisSession == null) {
			synchronized (sessionMonitor) {
				if (cmisSession == null)
					cmisSession = sessionFactory.createSession(cmisProperties.getParameters());
			}
		}
		return cmisSession;
	}

	@Override
	public ProviderDriverProtocol getSupportedProtocol() {
		return ProviderDriverProtocol.CMIS_REMOTE;
	}

	@Override
	public Mono<PageLayoutDefinition> resolvePageLayout(String pagePath) {
		return ReactiveExecutionUtil.executeWithFallback(
				() -> contentExtractor.parsePageLayout(pagePath, getSession().getObjectByPath(pagePath), templateName),
				CmisBaseException.class,
				e -> ProviderDriverFallbackProvider.fallbackLayoutDefinition(pagePath, e)
		);
	}

	@Override
	public Mono<ContentComponent> resolveComponent(String componentPath) {
		return ReactiveExecutionUtil.executeWithFallback(
				() -> contentExtractor.parseComponent(componentPath, getSession().getObjectByPath(componentPath)),
				CmisBaseException.class,
				e -> ProviderDriverFallbackProvider.fallbackComponent(componentPath, e)
		);
	}
}
