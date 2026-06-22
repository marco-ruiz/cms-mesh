package net.bopsys.cms.mesh.engine;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.spi.ProviderDriver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry to find and resolve ProviderDrivers.
 *
 * @author Marco Ruiz
 */
public class ProviderDriversRegistry {

	private final Map<ProviderDriverProtocol, ProviderDriver> providers;
	private final ProviderDriver defaultProviderDriver;

	public ProviderDriversRegistry(ProviderDriverProtocol defaultProviderId, List<ProviderDriver> providerDrivers) {
		this.providers = providerDrivers.stream()
				.collect(Collectors.toMap(ProviderDriver::getSupportedProtocol, p -> p));
		ProviderDriver provider = providers.get(defaultProviderId);
		this.defaultProviderDriver = (provider == null && !providers.isEmpty()) ?
				providers.values().iterator().next() : provider;
	}

	/**
	 * Resolves provider using active protocol key or falls back to default.
	 *
	 * @param activeProviderId active requested protocol key
	 * @return content provider instance safely resolved
	 */
	public ProviderDriver getProvider(ProviderDriverProtocol activeProviderId) {
		if (activeProviderId == null)
			return defaultProviderDriver;

		ProviderDriver provider = providers.get(activeProviderId);
		return (provider != null) ? provider : defaultProviderDriver;
	}
}
