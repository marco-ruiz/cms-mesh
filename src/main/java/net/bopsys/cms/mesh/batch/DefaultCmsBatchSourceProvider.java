package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of CmsBatchSourceProvider loading paths and protocol from configuration properties.
 *
 * @author Marco Ruiz
 */
@Component
public class DefaultCmsBatchSourceProvider implements CmsBatchSourceProvider {

    private final CmsBatchProperties batchProperties;

    public DefaultCmsBatchSourceProvider(CmsBatchProperties batchProperties) {
        this.batchProperties = batchProperties;
    }

    @Override
    public List<String> getTargetPaths() {
        return batchProperties.targetPaths() != null ? batchProperties.targetPaths() : List.of();
    }

    @Override
    public ProviderDriverProtocol getTargetProtocol() {
        return batchProperties.defaultProtocol() != null ? 
                batchProperties.defaultProtocol() : 
                ProviderDriverProtocol.LOCAL_JCR;
    }
}