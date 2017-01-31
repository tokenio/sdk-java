package io.token.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication context. Stores the value of On-Behalf-Of in the
 * thread local storage to be used for request authentication.
 */
public class AuthenticationContext {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationContext.class);
    private static final ThreadLocal<String> onBehalfOf = new ThreadLocal<>();

    /**
     * Retrieves the On-Behalf-Of value.
     *
     * @return the current On-Behalf-Of value
     */
    public static String getOnBehalfOf() {
        return onBehalfOf.get();
    }

    /**
     * Sets the On-Behalf-Of value.
     *
     * @param tokenId the value of the On-Behalf-Of
     */
    public static void setOnBehalfOf(String tokenId) {
        logger.info("Authenticated On-Behalf-Of: {}", tokenId);
        onBehalfOf.set(tokenId);
    }

    /**
     * Retrieves and clears an On-Behalf-Of value.
     *
     * @return an On-Behalf-Of value or null
     */
    public static String clearOnBehalfOf() {
        String tokenId = onBehalfOf.get();
        onBehalfOf.remove();
        return tokenId;
    }

    /**
     * Resets the authenticator.
     */
    public static void clear() {
        onBehalfOf.remove();
    }
}
