package net.bopsys.cms.mesh.batch;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Chunk listener for tracking chunk processing duration, success rates and performance metrics.
 *
 * @author Marco Ruiz
 */
@Slf4j
@Component
public class CmsBatchAuditListener implements ChunkListener {

    private long startTime;

    @Override
    public void beforeChunk(@NonNull ChunkContext context) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long duration = System.currentTimeMillis() - startTime;
        String stepName = context.getStepContext().getStepName();
        log.debug("Batch Step '{}' processed chunk successfully in {} ms", stepName, duration);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        String stepName = context.getStepContext().getStepName();
        log.error("Fatal unhandled error encountered inside chunk execution for step '{}'", stepName);
    }
}