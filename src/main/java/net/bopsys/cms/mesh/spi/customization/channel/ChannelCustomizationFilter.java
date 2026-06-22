package net.bopsys.cms.mesh.spi.customization.channel;

import net.bopsys.cms.mesh.config.YamlPropertySourceFactory;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.CustomizationFilter;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies data-driven rule mutations based on registered Channel Profiles.
 *
 * @author Marco Ruiz
 */
@Component
@Order(2)
@PropertySource(value = "classpath:channel-profiles.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "profiles")
public class ChannelCustomizationFilter implements CustomizationFilter {

	@Getter
	private Map<String, ChannelProfile> profileRegistry = new HashMap<>();

	public void setProfileRegistry(Map<String, ChannelProfile> profileRegistry) {
		this.profileRegistry = profileRegistry != null ? profileRegistry : new HashMap<>();
	}

	@Override
	public Mono<ContentComponent> applyCustomization(ContentComponent incoming, UserContext ctx) {
		return Mono.fromCallable(() -> applyCustomizationSync(incoming, ctx));
	}

	@Nonnull
	private ContentComponent applyCustomizationSync(ContentComponent incoming, UserContext ctx) {
		String channelId = ctx.get(ContextKey.CHANNEL_ID).orElse(null);
		return (channelId != null && profileRegistry.containsKey(channelId)) ?
				incoming.buildCopyWithState(profileRegistry.get(channelId).createState(incoming)) :
				incoming;
	}
}
