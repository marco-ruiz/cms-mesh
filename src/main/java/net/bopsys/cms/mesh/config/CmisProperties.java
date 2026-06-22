package net.bopsys.cms.mesh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CMIS properties abstraction containing connection parameters mapping.
 *
 * @author Marco Ruiz
 */
@ConfigurationProperties(prefix = "cms.mesh.cmis")
public record CmisProperties(
    String user, 
    String password, 
    String url, 
    String bindingType, 
    String repositoryId, 
    String templateName
) {
    // CMIS Configuration Keys
    private static final String USER_KEY = "org.apache.chemistry.opencmis.user";
    private static final String PWD_KEY = "org.apache.chemistry.opencmis.password";
    private static final String URL_KEY = "org.apache.chemistry.opencmis.binding.atompub.url";
    private static final String SPI_KEY = "org.apache.chemistry.opencmis.binding.spi.type";
    private static final String REPO_KEY = "org.apache.chemistry.opencmis.session.repository.id";

    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();

        putIfPresent(parameters, USER_KEY, user);
        putIfPresent(parameters, PWD_KEY, password);
        putIfPresent(parameters, URL_KEY, url);
        putIfPresent(parameters, REPO_KEY, repositoryId);
        
        // Handle fallback value natively
        parameters.put(SPI_KEY, Objects.requireNonNullElse(bindingType, "atompub"));

        return Map.copyOf(parameters);
    }
    
    public String getTemplateName() {
        return Objects.requireNonNullElse(templateName, "remote-cmis-template");
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        Optional.ofNullable(value).ifPresent(v -> map.put(key, v));
    }
}
