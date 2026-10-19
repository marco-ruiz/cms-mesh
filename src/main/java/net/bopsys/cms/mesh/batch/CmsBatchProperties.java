package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "cms.mesh.batch")
public record CmsBatchProperties(
    boolean enabled,
    int chunkSize,
    int gridSize,
    int threadPoolSize,
    int skipLimit,
    long retryIntervalMs,
    List<String> targetPaths,
    ProviderDriverProtocol defaultProtocol
) {
    public CmsBatchProperties {
        if (chunkSize <= 0) chunkSize = 20;
        if (gridSize <= 0) gridSize = 4;
        if (threadPoolSize <= 0) threadPoolSize = 10;
        if (skipLimit < 0) skipLimit = 5;
        if (retryIntervalMs <= 0) retryIntervalMs = 1000;
        if (targetPaths == null) targetPaths = List.of("/content/home", "/content/products");
    }
}