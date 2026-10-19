package net.bopsys.cms.mesh.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Batch infrastructure configuration providing DataSource, TransactionManager,
 * and enabling Spring Batch processing to prevent auto-configuration backoff issues.
 *
 * @author Marco Ruiz
 */
@Configuration
@EnableBatchProcessing
public class BatchInfrastructureConfig {

    @Bean(name = "dataSource")
    public DataSource batchDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:org/springframework/batch/core/schema-h2.sql")
                .build();
    }

	@Bean(name = "transactionManager")
    public PlatformTransactionManager batchTransactionManager(DataSource batchDataSource) {
        return new DataSourceTransactionManager(batchDataSource);
    }
}