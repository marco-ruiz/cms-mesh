package net.bopsys.cms.mesh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * JCR configuration properties matching namespaces, login credentials, and mapping keys.
 *
 * @author Marco Ruiz
 */
@ConfigurationProperties(prefix = "cms.mesh.jcr")
public record JcrProperties(
    String user, 
    String password, 
    String slingNamespaceUri, 
    String slingNamespacePrefix, 
    String transientRepoDirPrefix, 
    String repositoryXmlName,
    String slingTemplateProperty,
    String cqTemplateProperty,
    String slingResourceTypeProperty,
    String slingResourceSuperTypeProperty
) {
    public String getUser() {
        return Objects.requireNonNullElse(user, "admin");
    }

    public String getPassword() {
        return Objects.requireNonNullElse(password, "admin");
    }

    public String getSlingNamespaceUri() {
        return Objects.requireNonNullElse(slingNamespaceUri, "http://sling.apache.org/jcr/sling/1.0");
    }

    public String getSlingNamespacePrefix() {
        return Objects.requireNonNullElse(slingNamespacePrefix, "sling");
    }

    public String getTransientRepoDirPrefix() {
        return Objects.requireNonNullElse(transientRepoDirPrefix, "jcr-repo");
    }

    public String getRepositoryXmlName() {
        return Objects.requireNonNullElse(repositoryXmlName, "repository.xml");
    }

    public String getSlingTemplateProperty() {
        return Objects.requireNonNullElse(slingTemplateProperty, getSlingNamespacePrefix() + ":template");
    }

    public String getCqTemplateProperty() {
        return Objects.requireNonNullElse(cqTemplateProperty, "cq:template");
    }

    public String getSlingResourceTypeProperty() {
        return Objects.requireNonNullElse(slingResourceTypeProperty, getSlingNamespacePrefix() + ":resourceType");
    }

    public String getSlingResourceSuperTypeProperty() {
        return Objects.requireNonNullElse(slingResourceSuperTypeProperty, 
                getSlingNamespacePrefix() + ":resourceSuperType");
    }
}
