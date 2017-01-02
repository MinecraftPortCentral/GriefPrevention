package me.ryanhamshire.griefprevention;

import me.ryanhamshire.griefprevention.api.GriefPreventionApi;

public final class GriefPrevention {

    private static final GriefPreventionApi api = null;

    /**
     * Gets the API instance of {@link GriefPreventionAPI}
     *
     * @return The API instance, if available
     * @throws IllegalStateException if the API is not loaded
     */
    public static GriefPreventionApi getApi() {
        if (api == null) {
            throw new IllegalStateException("The GriefPrevention API is not loaded.");
        }
        return api;
    }
}
