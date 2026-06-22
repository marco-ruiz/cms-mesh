package net.bopsys.cms.mesh.util;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Reactive offloader execution helper targeting bounded elastic threads.
 *
 * @author Marco Ruiz
 */
public final class ReactiveExecutionUtil {

    private ReactiveExecutionUtil() {}

    public static <T, E extends Throwable> Mono<T> executeWithFallback(
            Callable<T> action,
            Class<E> errorType,
            Function<E, Mono<T>> fallback) {

        return Mono.fromCallable(action)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(errorType, fallback);
    }
}
