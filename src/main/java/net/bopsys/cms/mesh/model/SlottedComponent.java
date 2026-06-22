package net.bopsys.cms.mesh.model;

/**
 * Association container for a content component mapped to its layout slot.
 *
 * @author Marco Ruiz
 */
public record SlottedComponent(String slotName, ContentComponent component) {
}
