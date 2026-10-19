package net.bopsys.cms.mesh.config;

import net.bopsys.cms.mesh.batch.CmsBatchProperties;
import net.bopsys.cms.mesh.engine.ProviderDriversRegistry;
import net.bopsys.cms.mesh.engine.CustomizationFiltersRegistry;
import net.bopsys.cms.mesh.engine.LayoutAssembler;
import net.bopsys.cms.mesh.engine.LayoutCache;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import net.bopsys.cms.mesh.spi.CustomizationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Main application wiring and configuration class.
 *
 * @author Marco Ruiz
 */
@Configuration
@EnableConfigurationProperties(
		{MeshProperties.class, CmisProperties.class, JcrProperties.class, AemProperties.class, CmsBatchProperties.class}
)
public class MeshConfig {

	@Bean
	public ProviderDriversRegistry providerDriverRegistry(
			MeshProperties props,
			List<ProviderDriver> providerDrivers) {
		return new ProviderDriversRegistry(props.defaultProviderDriver(), providerDrivers);
	}

	@Bean
	public LayoutCache layoutCache(MeshProperties props) {
		return new LayoutCache(props.cache());
	}

	@Bean
	public CustomizationFiltersRegistry customizationFiltersRegistry(List<CustomizationFilter> customizationFilters) {
		return new CustomizationFiltersRegistry(customizationFilters);
	}

	@Bean
	public LayoutAssembler layoutAssembler(
			ProviderDriversRegistry providerRegistry,
			LayoutCache layoutCache,
			CustomizationFiltersRegistry customizationFiltersRegistry) {
		return new LayoutAssembler(providerRegistry, customizationFiltersRegistry, layoutCache);
	}
}
