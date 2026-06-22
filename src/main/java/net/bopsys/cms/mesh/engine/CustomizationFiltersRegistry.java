package net.bopsys.cms.mesh.engine;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.CustomizationFilter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

public class CustomizationFiltersRegistry {
    
    private final List<CustomizationFilter> customizationFilters;
    private final List<BiFunction<ContentComponent, UserContext, Mono<ContentComponent>>> pipelines;

    public CustomizationFiltersRegistry(List<CustomizationFilter> filters) {
        this.customizationFilters = filters != null ? filters : List.of();
        this.pipelines = this.customizationFilters.stream()
                .<BiFunction<ContentComponent, UserContext, Mono<ContentComponent>>>map(p -> p::applyCustomization)
                .toList();
    }

    public Mono<ContentComponent> applyFilters(ContentComponent source, UserContext ctx) {
        return executePipeline(source, ctx, pipelines);
    }

    private <T, C> Mono<T> executePipeline(T source, C ctx, List<BiFunction<T, C, Mono<T>>> pipelines) {
        return pipelines.stream()
                .reduce(
                        Mono.just(source),
                        (mono, pipe) -> mono.flatMap(item -> pipe.apply(item, ctx)),
                        (m1, m2) -> m1.flatMap(ignored -> m2)
                );
    }
}
