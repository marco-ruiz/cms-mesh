package net.bopsys.cms.mesh.spi.content.jcr;

import lombok.extern.slf4j.Slf4j;
import net.bopsys.cms.mesh.config.JcrProperties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Executes operations inside a managed JCR session, ensuring login and safe cleanup.
 *
 * @author Marco Ruiz
 */
@Slf4j
public class JcrSessionManager {

	private final Repository repository;
	private final JcrProperties jcrProperties;

	public JcrSessionManager(Repository repository, JcrProperties jcrProperties) {
		this.repository = repository;
		this.jcrProperties = jcrProperties;
	}

	public <T> T execute(JcrCallback<T> action) throws RepositoryException {
		Session session = null;
		try {
			char[] pswd = jcrProperties.getPassword().toCharArray();
			session = repository.login(new SimpleCredentials(jcrProperties.getUser(), pswd));
			log.debug("JCR Session logged in successfully.");
			return action.doInSession(session);
		} finally {
			closeSession(session);
		}
	}

	private void closeSession(Session session) {
		if (session != null && session.isLive()) {
			session.logout();
			log.debug("JCR Session logged out successfully.");
		}
	}

	@FunctionalInterface
	public interface JcrCallback<T> {
		T doInSession(Session session) throws RepositoryException;
	}
}
