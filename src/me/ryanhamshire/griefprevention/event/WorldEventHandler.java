package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerDataWorldManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;

public class WorldEventHandler {

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        GriefPrevention.instance.dataStore.loadWorldData(event.getTargetWorld().getProperties());
    }

    @Listener
    public void onWorldUnload(UnloadWorldEvent event) {
        GriefPrevention.instance.dataStore.unloadWorldData(event.getTargetWorld().getProperties());
    }

    @Listener
    public void onWorldSave(SaveWorldEvent event) {
        PlayerDataWorldManager playerWorldManager = GriefPrevention.instance.dataStore.getPlayerDataWorldManager(event.getTargetWorld().getProperties());
        if (playerWorldManager == null) {
            return;
        }

        playerWorldManager.save();
    }
}
