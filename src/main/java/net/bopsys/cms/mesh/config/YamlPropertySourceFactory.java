package net.bopsys.cms.mesh.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 * Configuration factory allowing loading of structured YAML resource files.
 *
 * @author Marco Ruiz
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
	    String filename = (name != null) ? name : resource.getResource().getFilename();
	    return new PropertiesPropertySource(filename, factory.getObject());
    }
}
