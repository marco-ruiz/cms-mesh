package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized unit tests for CmsPagePartitioner sharding logic.
 *
 * @author Marco Ruiz
 */
class CmsPagePartitionerTest {

	@ParameterizedTest(name = "{index} => Total Paths: {0}, Grid Size: {1}, Expected Partitions: {2}")
	@MethodSource("partitioningScenarioProvider")
	@DisplayName("Parameterized test for content path partitioning sharding")
	void testPartitioningScenarios(List<String> paths, int gridSize, int expectedPartitionCount) {
		CmsPagePartitioner partitioner = new CmsPagePartitioner(createSourProvider(paths));
		Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

		assertThat(partitions).hasSize(expectedPartitionCount);

		if (!paths.isEmpty()) {
			int totalDistributedPaths = partitions.values().stream()
					.map(ctx -> (List<String>) ctx.get("partitionPaths"))
					.mapToInt(List::size)
					.sum();

			assertThat(totalDistributedPaths).isEqualTo(paths.size());
		}
	}

	private static Stream<Arguments> partitioningScenarioProvider() {
		return Stream.of(
				Arguments.of(List.of("/p1", "/p2", "/p3", "/p4", "/p5", "/p6"), 3, 3),
				Arguments.of(List.of("/p1", "/p2"), 5, 2), // Grid size larger than path count
				Arguments.of(List.of(), 4, 0),              // Empty path collection
				Arguments.of(List.of("/single"), 1, 1)      // Single path item
		);
	}

	@Test
	@DisplayName("Should correctly partition paths into exact grid sizes with serializable lists")
	void testPartitioningWithValidGridSize() {
		List<String> paths = List.of("/p1", "/p2", "/p3", "/p4", "/p5", "/p6");
		CmsPagePartitioner partitioner = new CmsPagePartitioner(createSourProvider(paths));

		Map<String, ExecutionContext> partitions = partitioner.partition(3);

		assertThat(partitions).hasSize(3);
		assertThat(partitions).containsKey("partition-0");
		assertThat(partitions).containsKey("partition-1");
		assertThat(partitions).containsKey("partition-2");

		// Verify partition-0 content and serialization contract
		List<String> p0Paths = (List<String>) partitions.get("partition-0").get("partitionPaths");
		assertThat(p0Paths).isInstanceOf(ArrayList.class);
		assertThat(p0Paths).containsExactly("/p1", "/p2");

		// Verify partition metadata name
		assertThat(partitions.get("partition-0").getString("partitionName")).isEqualTo("partition-0");
	}

	@Test
	@DisplayName("Should return empty partition map when target path collection is empty")
	void testPartitioningWithEmptyPaths() {
		CmsPagePartitioner partitioner = new CmsPagePartitioner(createSourProvider(List.of()));
		Map<String, ExecutionContext> partitions = partitioner.partition(4);

		assertThat(partitions).isEmpty();
	}

	@ParameterizedTest(name = "Grid size {0} with {1} paths should result in {2} active partitions")
	@CsvSource({
			"5, 2, 2", // Grid size larger than path count clamps to path count
			"1, 4, 1", // Single grid size processes all paths in one partition
			"4, 4, 4"  // Equal grid size and path count
	})
	@DisplayName("Parameterized unit test for grid size boundary conditions")
	void testPartitioningBoundaryConditions(int gridSize, int pathCount, int expectedPartitions) {
		List<String> paths = new ArrayList<>();
		for (int i = 1; i <= pathCount; i++) {
			paths.add("/content/page-" + i);
		}

		CmsPagePartitioner partitioner = new CmsPagePartitioner(createSourProvider(paths));
		Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

		assertThat(partitions).hasSize(expectedPartitions);
	}

	private CmsBatchSourceProvider createSourProvider(List<String> paths) {
		return new CmsBatchSourceProvider() {
			@Override
			public List<String> getTargetPaths() {
				return paths;
			}

			@Override
			public ProviderDriverProtocol getTargetProtocol() {
				return null;
			}
		};
	}
}