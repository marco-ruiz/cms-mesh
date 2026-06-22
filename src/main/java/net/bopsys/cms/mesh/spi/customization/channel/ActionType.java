package net.bopsys.cms.mesh.spi.customization.channel;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Strategy-pattern action types to process component state mutations.
 *
 * @author Marco Ruiz
 */
public enum ActionType {

	APPEND(ActionType::appendState),
	PREPEND(ActionType::prependState),
	REMOVE(ActionType::removeState),
	OVERWRITE(ActionType::putState),
	SET(ActionType::putState);

	private final BiConsumer<Map<String, Object>, ChannelProfile.TransformationRule> stateStrategy;

	ActionType(BiConsumer<Map<String, Object>, ChannelProfile.TransformationRule> stateStrategy) {
		this.stateStrategy = stateStrategy;
	}

	public void executeOn(Map<String, Object> state, ChannelProfile.TransformationRule rule) {
		stateStrategy.accept(state, rule);
	}

	private static void appendState(Map<String, Object> state, ChannelProfile.TransformationRule r) {
		state.merge(r.targetKey(), r.modificationValue(), (o, n) -> String.valueOf(o) + n);
	}

	private static void prependState(Map<String, Object> state, ChannelProfile.TransformationRule r) {
		state.merge(r.targetKey(), r.modificationValue(), (o, n) -> String.valueOf(n) + o);
	}

	private static void removeState(Map<String, Object> state, ChannelProfile.TransformationRule r) {
		state.remove(r.targetKey());
	}

	private static void putState(Map<String, Object> state, ChannelProfile.TransformationRule r) {
		state.put(r.targetKey(), r.modificationValue());
	}
}
