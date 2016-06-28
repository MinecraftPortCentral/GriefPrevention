/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.claim.ClaimWorldManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;

public class WorldEventHandler {

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        GPTimings.WORLD_LOAD_EVENT.startTimingIfSync();
        GriefPrevention.instance.dataStore.loadWorldData(event.getTargetWorld());
        GPTimings.WORLD_LOAD_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onWorldUnload(UnloadWorldEvent event) {
        GPTimings.WORLD_UNLOAD_EVENT.startTimingIfSync();
        GriefPrevention.instance.dataStore.unloadWorldData(event.getTargetWorld().getProperties());
        GPTimings.WORLD_UNLOAD_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onWorldSave(SaveWorldEvent event) {
        GPTimings.WORLD_SAVE_EVENT.startTimingIfSync();
        ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(event.getTargetWorld().getProperties());
        if (claimWorldManager == null) {
            GPTimings.WORLD_SAVE_EVENT.stopTimingIfSync();
            return;
        }

        claimWorldManager.save();
        GPTimings.WORLD_SAVE_EVENT.stopTimingIfSync();
    }
}
