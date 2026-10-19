package net.bopsys.cms.mesh.batch;

import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import java.util.List;

/**
 * SPI for supplying target content paths and provider protocols to batch pre-warming and ingestion jobs.
 * Downstream developers can implement this interface to integrate custom repository traversal logic.
 *
 * @author Marco Ruiz
 */
public interface CmsBatchSourceProvider {

    /**
     * Retrieves the collection of content paths targeted for batch processing.
     *
     * @return list of content path strings
     */
    List<String> getTargetPaths();

    /**
     * Returns the target provider protocol used for resolving batch items.
     *
     * @return ProviderDriverProtocol enum
     */
    ProviderDriverProtocol getTargetProtocol();
}