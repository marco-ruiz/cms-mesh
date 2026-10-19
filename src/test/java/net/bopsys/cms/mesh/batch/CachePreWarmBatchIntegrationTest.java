package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.engine.LayoutAssembler;
import net.bopsys.cms.mesh.engine.LayoutCache;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Integration test suite validating successful cache pre-warming and skip-limit threshold breach failures.
 *
 * @author Marco Ruiz
 */
@SpringBatchTest
@SpringBootTest
class CachePreWarmBatchIntegrationTest {

   @Autowired
   private JobLauncherTestUtils jobLauncherTestUtils;

   @MockitoBean
   private LayoutAssembler layoutAssembler;

   @MockitoBean
   private LayoutCache layoutCache;

   @MockitoBean
   private CmsBatchSourceProvider sourceProvider;

   @BeforeEach
   void setUp() {
      PageLayoutDefinition mockDef = PageLayoutDefinition.builder()
            .pagePath("/content/home")
            .templateName("default")
            .slots(Map.of("header", new SlotGroupDefinition("header", List.of("/comp/1"))))
            .build();

      net.bopsys.cms.mesh.model.PageLayout mockLayout = net.bopsys.cms.mesh.model.PageLayout.builder()
            .definition(mockDef)
            .slots(Map.of())
            .build();

      lenient().when(layoutAssembler.assemblePageLayout(any(), any(), any()))
            .thenReturn(Mono.just(mockLayout));
   }

   @ParameterizedTest(name = "{index} => Protocol: {0}, Target Paths: {1}")
   @MethodSource("happyPathScenarios")
   @DisplayName("Parameterized integration test for successful cache pre-warming across protocols")
   void testCachePreWarmJobSuccess(
         ProviderDriverProtocol protocol,
         List<String> targetPaths) throws Exception {

      when(sourceProvider.getTargetProtocol()).thenReturn(protocol);
      when(sourceProvider.getTargetPaths()).thenReturn(targetPaths);

      JobExecution jobExecution = jobLauncherTestUtils.launchJob();

      assertThat(jobExecution).isNotNull();
      assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
      assertThat(jobExecution.getStepExecutions()).isNotEmpty();

      jobExecution.getStepExecutions().forEach(stepExecution ->
            assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED)
      );
   }

   private static Stream<Arguments> happyPathScenarios() {
      return Stream.of(
            Arguments.of(ProviderDriverProtocol.LOCAL_JCR, List.of("/content/home", "/content/products")),
            Arguments.of(ProviderDriverProtocol.AEM_GRAPHQL, List.of("/content/aem/home")),
            Arguments.of(ProviderDriverProtocol.CMIS_REMOTE, List.of("/cmis/doc")),
            Arguments.of(ProviderDriverProtocol.LOCAL_JCR, List.of())
      );
   }

   @ParameterizedTest(name = "{index} => Protocol: {0}, Failing Path Count: {1}")
   @MethodSource("failureScenarios")
   @DisplayName("Parameterized integration test validating job failure when skip limits are exhausted")
   void testCachePreWarmJobFailureExceedingSkipLimit(
         ProviderDriverProtocol protocol,
         int failingPathCount) throws Exception {

      // Build a large volume of failing paths guaranteed to exceed any skip limit threshold
      List<String> targetPaths = new ArrayList<>();
      for (int i = 1; i <= failingPathCount; i++) {
         targetPaths.add("/content/fail-" + i);
      }

      when(sourceProvider.getTargetProtocol()).thenReturn(protocol);
      when(sourceProvider.getTargetPaths()).thenReturn(targetPaths);

      // Force assembler to throw upstream errors
      when(layoutAssembler.assemblePageLayout(any(), any(), any()))
            .thenReturn(Mono.error(new RuntimeException("Critical Upstream CMS Outage")));

      JobExecution jobExecution = jobLauncherTestUtils.launchJob();
	   // --- DIAGNOSTIC METRICS ---
	   jobExecution.getStepExecutions().forEach(step -> {
		   System.out.println("Step Name: " + step.getStepName());
		   System.out.println("Status: " + step.getStatus());
		   System.out.println("Read Count: " + step.getReadCount());
		   System.out.println("Skip Count: " + step.getSkipCount());
		   System.out.println("Failure Exceptions: " + step.getFailureExceptions());
	   });
      assertThat(jobExecution).isNotNull();
      assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

      boolean hasFailedStep = jobExecution.getStepExecutions().stream()
            .anyMatch(step -> step.getStatus() == BatchStatus.FAILED);
      assertThat(hasFailedStep).isTrue();
   }

   private static Stream<Arguments> failureScenarios() {
      return Stream.of(
            // Passing 100 paths guarantees exhaustion of the skip limit, triggering FAILED
            Arguments.of(ProviderDriverProtocol.LOCAL_JCR, 14),
            Arguments.of(ProviderDriverProtocol.AEM_GRAPHQL, 25)
      );
   }
}