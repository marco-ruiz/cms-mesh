package net.bopsys.cms.mesh.config;

import net.bopsys.cms.mesh.spi.content.jcr.JcrRepositoryFactory;
import net.bopsys.cms.mesh.spi.content.jcr.JcrSessionManager;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jcr.Repository;

/**
 * Initializes infrastructure beans including JCR Repositories and Session Managers.
 *
 * @author Marco Ruiz
 */
@Configuration
public class CmsInfrastructureConfig {

	/**
	 * Initializes a Transient JCR Repository with matching default JCR configurations.
	 *
	 * @param jcrProperties configuration properties containing usernames and workspace definitions
	 * @return instanced JCR Repository bean
	 * @throws Exception on disk or namespace registration errors
	 */
	@Bean
	public Repository jcrRepository(JcrProperties jcrProperties) throws Exception {
		return JcrRepositoryFactory.create(jcrProperties);
	}

	/**
	 * Creates a thread-safe wrapper managing login/logout JCR execution blocks.
	 *
	 * @param repository    target JCR registry bean
	 * @param jcrProperties properties for authentication credentials
	 * @return custom thread-safe wrapper managing login/logout JCR execution blocks
	 */
	@Bean
	public JcrSessionManager jcrSessionManager(Repository repository, JcrProperties jcrProperties) {
		return new JcrSessionManager(repository, jcrProperties);
	}

	/**
	 * Initializes CMIS compliant session manager factory.
	 *
	 * @return SessionFactory bean
	 */
	@Bean
	public SessionFactory cmisSessionFactory() {
		return SessionFactoryImpl.newInstance();
	}
}
