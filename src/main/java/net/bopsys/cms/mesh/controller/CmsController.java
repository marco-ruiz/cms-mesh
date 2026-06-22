package net.bopsys.cms.mesh.controller;

import net.bopsys.cms.mesh.engine.LayoutAssembler;
import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.PageLayout;
import net.bopsys.cms.mesh.model.SlottedComponent;
import net.bopsys.cms.mesh.model.ContextKey;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes REST API endpoints for the content aggregation mesh platform.
 *
 * @author Marco Ruiz
 */
@RestController
@RequestMapping("/api/v1")
public class CmsController {

    private final LayoutAssembler layoutAssembler;
    
    @Value("${cms.mesh.default-provider:LOCAL_JCR}")
    private ProviderDriverProtocol defaultProviderDriver;

    public CmsController(LayoutAssembler layoutAssembler) {
        this.layoutAssembler = layoutAssembler;
    }

    /**
     * Resolves layout maps in-memory grouping slotted components.
     *
     * @param path target CMS page path
     * @param provider overriding provider protocol selector
     * @param tier customer access scope criteria
     * @param channelId target delivery channel ID
     * @return Mono enclosing complete mapped slots
     */
    @GetMapping(value = "/page/structure", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<PageLayout> getPageLayout(
            @RequestParam String path,
            @RequestParam(required = false) ProviderDriverProtocol provider,
            @RequestHeader(required = false) String tier,
            @RequestHeader(required = false) String channelId) {
            
        return layoutAssembler.assemblePageLayout(path, buildContext(tier, channelId), resolveProvider(provider));
    }

    /**
     * Retrieves flat individual resolved slots configurations.
     *
     * @param path target CMS page path
     * @param provider custom overriding provider protocol selector
     * @param tier customer tier access scope criteria
     * @param channelId target delivery channel ID
     * @return Mono with list of individual component entries
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<SlottedComponent>> getPage(
            @RequestParam String path,
            @RequestParam(required = false) ProviderDriverProtocol provider,
            @RequestHeader(required = false) String tier,
            @RequestHeader(required = false) String channelId) {
            
        return layoutAssembler.streamPageLayout(path, buildContext(tier, channelId), resolveProvider(provider)).collectList();
    }

    /**
     * Streams individual resolved slots configurations over Server-Sent Events.
     *
     * @param path target CMS page path
     * @param provider custom overriding provider protocol selector
     * @param tier customer tier access scope criteria
     * @param channelId target delivery channel ID
     * @return Flux streaming individual component entries
     */
    @GetMapping(value = "/page/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<SlottedComponent> getPageStream(
            @RequestParam String path,
            @RequestParam(required = false) ProviderDriverProtocol provider,
            @RequestHeader(required = false) String tier,
            @RequestHeader(required = false) String channelId) {
            
        return layoutAssembler.streamPageLayout(path, buildContext(tier, channelId), resolveProvider(provider));
    }

	private UserContext buildContext(String tier, String channelId) {
		Map<ContextKey, String> contextMap = new HashMap<>();
		if (tier != null) contextMap.put(ContextKey.TIER, tier);
		if (channelId != null) contextMap.put(ContextKey.CHANNEL_ID, channelId);
		return new UserContext(contextMap);
	}

	private ProviderDriverProtocol resolveProvider(ProviderDriverProtocol provider) {
		return provider != null ? provider : defaultProviderDriver;
	}

    /**
     * Triggers Cache invalidations across registered layout profiles matching tags.
     *
     * @param tags dynamic target collection
     * @return completion signal
     */
    @PostMapping(value = "/cache/invalidate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Void> invalidateCache(@RequestBody List<String> tags) {
        return layoutAssembler.invalidateCache(tags);
    }
}
