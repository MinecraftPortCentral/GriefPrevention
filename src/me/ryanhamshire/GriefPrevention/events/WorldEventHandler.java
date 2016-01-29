package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CleanupUnusedClaimsTask;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.DimensionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ConstructWorldEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.world.DimensionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorldEventHandler {

    private final Path rootConfigPath = Sponge.getGame().getSavesDirectory().resolve("config").resolve("GriefPrevention").resolve("worlds");

    @Listener
    public void onWorldLoad(ConstructWorldEvent event) {
        DimensionType dimType = event.getWorldProperties().getDimensionType();
        if (!Files.exists(rootConfigPath.resolve(dimType.getId()).resolve(event.getWorldProperties().getWorldName()))) {
            try {
                Files.createDirectories(rootConfigPath.resolve(dimType.getId()).resolve(event.getWorldProperties().getWorldName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // create/load configs
        // create dimension config
        DataStore.dimensionConfigMap.put(event.getWorldProperties().getUniqueId(), new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, rootConfigPath.resolve(dimType.getId()).resolve("dimension.conf")));
        // create world config
        DataStore.worldConfigMap.put(event.getWorldProperties().getUniqueId(), new GriefPreventionConfig<>(Type.WORLD, rootConfigPath.resolve(dimType.getId()).resolve(event.getWorldProperties().getWorldName()).resolve("world.conf")));
        // run cleanup task
        CleanupUnusedClaimsTask task = new CleanupUnusedClaimsTask(event.getWorldProperties());
        Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);
    }

    @Listener
    public void onWorldSave(SaveWorldEvent event) {
        List<Claim> claimList = GriefPrevention.instance.dataStore.worldClaims.get(event.getTargetWorld().getUniqueId());
        if (claimList != null) {
            for (Claim claim : claimList) {
                claim.claimData.save();
            }
        }
    }
}
