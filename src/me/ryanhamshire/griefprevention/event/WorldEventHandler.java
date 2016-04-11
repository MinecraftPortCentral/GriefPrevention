package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.DimensionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.task.CleanupUnusedClaimsTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
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
        DataStore.dimensionConfigMap.put(event.getWorldProperties().getUniqueId(),
                new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, rootConfigPath.resolve(dimType.getId()).resolve("dimension.conf")));
        // create world config
        DataStore.worldConfigMap.put(event.getWorldProperties().getUniqueId(), new GriefPreventionConfig<>(Type.WORLD,
                rootConfigPath.resolve(dimType.getId()).resolve(event.getWorldProperties().getWorldName()).resolve("world.conf")));
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

        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(event.getTargetWorld().getProperties(), player.getUniqueId());
            if (playerData != null) {
                PlayerStorageData storageData = playerData.worldStorageData.get(event.getTargetWorld().getUniqueId());
                if (storageData != null) {
                    storageData.save();
                }
            }
        }
    }
}
