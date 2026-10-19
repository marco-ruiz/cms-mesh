package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.engine.LayoutAssembler;
import net.bopsys.cms.mesh.engine.LayoutCache;
import net.bopsys.cms.mesh.model.PageLayout;
import net.bopsys.cms.mesh.model.UserContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Spring Batch infrastructure configuration for distributed cache pre-warming.
 * Decoupled from hardcoded paths and providers using the CmsBatchSourceProvider SPI.
 *
 * @author Marco Ruiz
 */
@Configuration
@EnableConfigurationProperties(CmsBatchProperties.class)
public class CachePreWarmBatchConfig {

    private final CmsBatchProperties batchProperties;
    private final CmsBatchSourceProvider sourceProvider;

    public CachePreWarmBatchConfig(CmsBatchProperties batchProperties, CmsBatchSourceProvider sourceProvider) {
        this.batchProperties = batchProperties;
        this.sourceProvider = sourceProvider;
    }

    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProperties.threadPoolSize());
        executor.setMaxPoolSize(batchProperties.threadPoolSize() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cms-batch-exec-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Job cachePreWarmJob(JobRepository jobRepository, Step masterPreWarmStep) {
        return new JobBuilder("cachePreWarmJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(masterPreWarmStep)
                .build();
    }

    @Bean
    public Step masterPreWarmStep(JobRepository jobRepository, TaskExecutor batchTaskExecutor, Step workerPreWarmStep) {
        return new StepBuilder("masterPreWarmStep", jobRepository)
                .partitioner("workerPreWarmStep", new CmsPagePartitioner(sourceProvider))
                .step(workerPreWarmStep)
                .gridSize(batchProperties.gridSize())
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step workerPreWarmStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> workerItemReader,
            AsyncItemProcessor<String, PageLayout> asyncItemProcessor,
            AsyncItemWriter<PageLayout> asyncItemWriter,
            CmsBatchAuditListener auditListener) {

        return new StepBuilder("workerPreWarmStep", jobRepository)
                .<String, Future<PageLayout>>chunk(batchProperties.chunkSize(), transactionManager)
                .reader(workerItemReader)
                .processor(asyncItemProcessor)
                .writer(asyncItemWriter)
                .listener(auditListener)
                .faultTolerant()
                .skip(Exception.class)
		        .noSkip(IllegalArgumentException.class)
                .skipLimit(batchProperties.skipLimit())
                .retry(RuntimeException.class)
                .retryLimit(3)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<String> workerItemReader(@Value("#{stepExecutionContext['partitionPaths']}}") List<String> partitionPaths) {
        return new ListItemReader<>(partitionPaths != null ? partitionPaths : List.of());
    }

    @Bean
    public ItemProcessor<String, PageLayout> standardLayoutProcessor(LayoutAssembler layoutAssembler) {
        return path -> {
            UserContext defaultContext = new UserContext(Map.of());
            return layoutAssembler.assemblePageLayout(path, defaultContext, sourceProvider.getTargetProtocol())
                    .block();
        };
    }

    @Bean
    public AsyncItemProcessor<String, PageLayout> asyncItemProcessor(
            ItemProcessor<String, PageLayout> standardLayoutProcessor,
            TaskExecutor batchTaskExecutor) {
        AsyncItemProcessor<String, PageLayout> asyncProcessor = new AsyncItemProcessor<>();
        asyncProcessor.setDelegate(standardLayoutProcessor);
        asyncProcessor.setTaskExecutor(batchTaskExecutor);
        return asyncProcessor;
    }

    @Bean
    public ItemWriter<PageLayout> standardLayoutWriter(LayoutCache layoutCache) {
        return chunk -> {
            for (PageLayout layout : chunk.getItems()) {
                if (layout != null && layout.definition() != null) {
                    String cacheKey = String.join("||", layout.definition().pagePath(), sourceProvider.getTargetProtocol().name());
                    layoutCache.put(cacheKey, layout);
                }
            }
        };
    }

    @Bean
    public AsyncItemWriter<PageLayout> asyncItemWriter(ItemWriter<PageLayout> standardLayoutWriter) {
        AsyncItemWriter<PageLayout> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(standardLayoutWriter);
        return asyncWriter;
    }
}