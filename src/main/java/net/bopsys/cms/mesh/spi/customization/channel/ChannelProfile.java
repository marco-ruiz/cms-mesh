package net.bopsys.cms.mesh.spi.customization.channel;

import net.bopsys.cms.mesh.model.ContentComponent;
import jakarta.annotation.Nonnull;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Record encapsulating transformation rules applied natively per specific channel.
 *
 * @param channelId The target channel identifier
 * @param rules Transformation rules applying property mutations
 *
 * @author Marco Ruiz
 */
@Builder
public record ChannelProfile(String channelId, List<TransformationRule> rules) {

    /**
     * Rebuilds state dynamically with all transformation rules, ensuring absolute thread-safety.
     *
     * @param incoming the source content component
     * @return a thread-safe mutated map representing the component's state
     */
    @Nonnull
    public Map<String, Object> createState(ContentComponent incoming) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        if (incoming.state() != null)
            incoming.state().forEach((k, v) -> putIfValid(result, k, v));

        rules().forEach(rule -> rule.applyRule(result));
        return result;
    }

    private void putIfValid(Map<String, Object> map, String k, Object v) {
        if (k != null && v != null)
            map.put(k, v);
    }

    /**
     * Rule containing the targeted component state key and how it should be modified.
     *
     * @param targetKey the key targeted for modification
     * @param actionType the action strategy to use
     * @param modificationValue the value to inject or apply
     */
    @Builder
    public record TransformationRule(String targetKey, ActionType actionType, Object modificationValue) {

        /**
         * Applies the transformation rule to the provided state map.
         *
         * @param state the state map to modify
         */
        public void applyRule(Map<String, Object> state) {
            actionType.executeOn(state, this);
        }
    }
}
