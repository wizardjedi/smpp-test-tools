package com.a1systems.plugin;

/**
 * This interface contains methods for authorization mechanism.
 */
public interface Authorizer extends Plugin {
    /**
     * Authorize session with systemid and password. SMPP-mux try to auth
     * sessions with all registered authorizers. If authorizer will return
     * {@code}true{@code} then loop will be terminated and access will granted
     * to session.
     *
     * @param systemId
     * @param password
     * @return authoriztion status
     */
    public boolean auth(String systemId, String password);
}
