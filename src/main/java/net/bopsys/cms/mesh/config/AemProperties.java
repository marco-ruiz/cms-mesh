package net.bopsys.cms.mesh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cms.mesh.aem")
public record AemProperties(String graphqlEndpoint, String accessToken) {
}
