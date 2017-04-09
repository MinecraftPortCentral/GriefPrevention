package me.ryanhamshire.griefprevention;

import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import me.ryanhamshire.griefprevention.api.claim.Claim.Builder;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;
import java.util.UUID;

public class GPApiProvider implements GriefPreventionApi {

    public static final double API_VERSION = 0.2;

    public GPApiProvider() {
    }

    @Override
    public double getApiVersion() {
        return API_VERSION;
    }

    @Override
    public ClaimManager getClaimManager(WorldProperties worldProperties) {
        return GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(worldProperties);
    }

    @Override
    public Builder createClaimBuilder() {
        return new GPClaim.ClaimBuilder();
    }

    @Override
    public Optional<PlayerData> getGlobalPlayerData(UUID playerUniqueId) {
        return Optional.ofNullable(DataStore.GLOBAL_PLAYER_DATA.get(playerUniqueId));
    }

    @Override
    public Optional<PlayerData> getWorldPlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        return Optional.ofNullable(GriefPreventionPlugin.instance.dataStore.getPlayerData(worldProperties, playerUniqueId));
    }
}
