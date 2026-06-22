package net.bopsys.cms.mesh.util;

import java.util.function.Function;

/**
 * @author Marco Ruiz
 */
@FunctionalInterface
public interface UnsafeFunction<T, R, E extends Throwable> {
	R applyUnsafe(T target) throws E;

	default R applySafe(T target, String errorMsg) {
		return applySafe(target, e -> new RuntimeException(errorMsg, e));
	}

	default <N extends Throwable> R applySafe(T target, Function<Throwable, N> throwableFactory) throws N {
		try {
			return applyUnsafe(target);
		} catch (Throwable e) {
			throw throwableFactory.apply(e);
		}
	}

	/**
	 * Wraps a throwing extractor into a standard unchecked execution.
	 */
	static <T, R, E extends Throwable> R extractValue(T target, UnsafeFunction<T, R, E> extractor, String errorMsg) {
		return extractor.applySafe(target, errorMsg);
	}

	/**
	 * Wraps a throwing extractor into a standard unchecked execution.
	 */
	static <T, R, E extends Throwable, N extends Throwable> R extractValue(
			T target, UnsafeFunction<T, R, E> extractor, Function<Throwable, N> throwableFactory) throws N {

		return extractor.applySafe(target, throwableFactory);
	}
}
