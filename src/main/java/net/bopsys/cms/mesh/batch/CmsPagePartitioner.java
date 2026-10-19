package net.bopsys.cms.mesh.batch;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamically partitions content repository paths into distinct execution contexts
 * at runtime by querying the active CmsBatchSourceProvider.
 *
 * @author Marco Ruiz
 */
public class CmsPagePartitioner implements Partitioner {

    private final CmsBatchSourceProvider sourceProvider;

    public CmsPagePartitioner(CmsBatchSourceProvider sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // Fetch target paths dynamically at runtime when the job actually launches
        List<String> targetPaths = sourceProvider != null && sourceProvider.getTargetPaths() != null 
                ? sourceProvider.getTargetPaths() 
                : List.of();

        Map<String, ExecutionContext> map = new HashMap<>();
        int totalSize = targetPaths.size();
        if (totalSize != 0)
			partition(gridSize, totalSize, targetPaths, map);

        return map;
    }

    private void partition(int gridSize, int totalSize, List<String> targetPaths, Map<String, ExecutionContext> map) {
        int targetGridSize = Math.min(gridSize, totalSize);
        int sliceSize = (int) Math.ceil((double) totalSize / targetGridSize);

        int start = 0;
        for (int i = 0; i < targetGridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            int end = Math.min(start + sliceSize, totalSize);

            // Wrap in new ArrayList to ensure serializability for ExecutionContext persistence
            List<String> subList = new ArrayList<>(targetPaths.subList(start, end));
            context.put("partitionPaths", subList);
            context.putString("partitionName", "partition-" + i);

            map.put("partition-" + i, context);
            start = end;
        }
    }
}