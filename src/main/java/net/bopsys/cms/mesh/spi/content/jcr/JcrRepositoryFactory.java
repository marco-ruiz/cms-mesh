package net.bopsys.cms.mesh.spi.content.jcr;

import lombok.extern.slf4j.Slf4j;
import net.bopsys.cms.mesh.config.JcrProperties;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * Factory for creating and registering JCR repositories.
 *
 * @author Marco Ruiz
 */
@Slf4j
public class JcrRepositoryFactory {

	public static Repository create(JcrProperties jcrProperties) throws Exception {
		File homeDir = Files.createTempDirectory(jcrProperties.getTransientRepoDirPrefix()).toFile();
		File configXml = extractConfigXml(homeDir, jcrProperties.getRepositoryXmlName());

		Repository repository = new TransientRepository(configXml, homeDir);
		initializeNamespace(repository, jcrProperties);

		return repository;
	}

	private static File extractConfigXml(File homeDir, String repoXmlName) throws IOException {
		File configXml = new File(homeDir, repoXmlName);
		try (InputStream in = RepositoryImpl.class.getResourceAsStream(repoXmlName)) {
			if (in != null)
				Files.copy(in, configXml.toPath());
			else
				throw new IOException("Repository configuration XML not found in classpath: " + repoXmlName);
		}
		return configXml;
	}

	private static void initializeNamespace(Repository repo, JcrProperties props) throws RepositoryException {
		new JcrSessionManager(repo, props).execute(session -> {
			ensureNamespaceRegistered(session.getWorkspace().getNamespaceRegistry(), props);
			return null;
		});
	}

	private static void ensureNamespaceRegistered(
			NamespaceRegistry registry, JcrProperties props) throws RepositoryException {

		try {
			registry.getPrefix(props.getSlingNamespaceUri());
		} catch (NamespaceException e) {
			registry.registerNamespace(
					props.getSlingNamespacePrefix(),
					props.getSlingNamespaceUri()
			);
			log.info("Registered '{}' namespace globally at startup.", props.getSlingNamespacePrefix());
		}
	}
}
