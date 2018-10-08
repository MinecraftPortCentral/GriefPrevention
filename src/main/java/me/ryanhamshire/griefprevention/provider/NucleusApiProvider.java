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
package me.ryanhamshire.griefprevention.provider;

import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.exceptions.PluginAlreadyRegisteredException;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.api.service.NucleusPrivateMessagingService;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Optional;

public class NucleusApiProvider {


    public NucleusApiProvider() {
    }

    public static Optional<NucleusPrivateMessagingService> getPrivateMessagingService() {
        return NucleusAPI.getPrivateMessagingService();
    }

    public void registerTokens() {
        NucleusMessageTokenService messageTokenService = NucleusAPI.getMessageTokenService();
        PluginContainer pc = GriefPreventionPlugin.instance.pluginContainer;
        final DataStore dataStore = GriefPreventionPlugin.instance.dataStore;
        try {
            messageTokenService.register(GriefPreventionPlugin.instance.pluginContainer,
                    (tokenInput, commandSource, variables) -> {
                        // Each token will require something like this.

                        // This token, town, will give the name of the town the player is currently in.
                        // Will be registered in Nucleus as "{{pl:griefprevention:town}}", with the shortened version of "{{town}}"
                        // This will return the name of the town the player is currently in.
                        if (tokenInput.equalsIgnoreCase("town") && commandSource instanceof Player) {
                            Player player = (Player) commandSource;
                            final GPPlayerData data = dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

                            // Shamelessly stolen from PlayerEventHandler
                            if (data.inTown) {
                                return dataStore.getClaimAtPlayer(data, player.getLocation()).getTownClaim().getTownData().getTownTag();
                            }
                        }

                        return Optional.empty();
                    });
        } catch (PluginAlreadyRegisteredException ignored) {
            // already been done.
        }

        // register {{town}} from {{pl:griefprevention:town}}
        messageTokenService.registerPrimaryToken("town", pc, "town");
    }
}
