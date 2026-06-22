package net.bopsys.cms.mesh.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wrapper for the user context, providing type-safe property resolving logic.
 *
 * @author Marco Ruiz
 */
public record UserContext(Map<ContextKey, String> attributes) {
    
    /**
     * Constructs a type-safe wrapper for the context attributes.
     *
     * @param attributes the collection containing parameters
     */
    public UserContext(Map<ContextKey, String> attributes) {
        this.attributes = attributes != null ?
                Map.copyOf(attributes) :
                Collections.emptyMap();
    }

    /**
     * Resolves the value tied to a ContextKey.
     *
     * @param key target lookup key
     * @return the Optional wrapped value
     */
    public Optional<String> get(ContextKey key) {
        return Optional.ofNullable(attributes.get(key));
    }
    
    /**
     * Validates whether attributes match expected strings.
     *
     * @param key key under inspection
     * @param expectedValue target content value
     * @return boolean evaluation matching outcome
     */
    public boolean is(ContextKey key, String expectedValue) {
        return get(key).map(v -> v.equals(expectedValue)).orElse(false);
    }
    
    /**
     * Generates a stable key fingerprint for cached targets.
     *
     * @return cache token string
     */
    public String fingerprint() {
	    return attributes.isEmpty() ? "" :
			    attributes.entrySet().stream()
			    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ContextKey::name)))
			    .map(e -> e.getKey().name() + "=" + e.getValue())
			    .collect(Collectors.joining(","));
    }
}
