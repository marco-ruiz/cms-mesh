package net.bopsys.cms.mesh.config;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root configuration parameters mapping standard cms.mesh namespace targets.
 *
 * @author Marco Ruiz
 */
@ConfigurationProperties(prefix = "cms.mesh")
public record MeshProperties(ProviderDriverProtocol defaultProviderDriver, CacheConfig cache) {

    public MeshProperties {
        if (cache == null) {
            cache = new CacheConfig(true, 50000, 15);
        }
    }

    public record CacheConfig(boolean enabled, long maxSize, long expireAfterWriteMinutes) {
        public CacheConfig {
            if (maxSize <= 0) maxSize = 50000;
            if (expireAfterWriteMinutes <= 0) expireAfterWriteMinutes = 15;
        }
    }
}
