package net.bopsys.cms.mesh.model;

/**
 * System-wide constants for CMS Mesh platform.
 * Eliminates hardcoded literal strings to ensure maintainability and SOLID standards.
 *
 * @author Marco Ruiz
 */
public final class CmsConstants {

    /** Default template name to use when template is not found or missing from node properties. */
    public static final String DEFAULT_TEMPLATE = "default-template";

    /** Key for error status or message. */
    public static final String KEY_ERROR = "error";

    /** Fallback template name. */
    public static final String FALLBACK_TEMPLATE = "error";

    /** Fallback or error component type. */
    public static final String FALLBACK_TYPE = "error/fallback";

    /** Default placeholder message for missing content components. */
    public static final String MISSING_MESSAGE = "Missing CMS Node";

    /** String represent of Member Tier VIP. */
    public static final String MEMBER_TIER_VIP = "VIP";

    /** VIP exclusive tag prefix. */
    public static final String VIP_PREFIX = "[VIP EXCLUSIVE] ";

    /** Key for layout slot. */
    public static final String SLOT_JCR_CONTENT = "jcr:content";

    private CmsConstants() {}
}
