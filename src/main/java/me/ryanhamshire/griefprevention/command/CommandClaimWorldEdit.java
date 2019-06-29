/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.griefprevention.command;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashSet;
import java.util.Set;

public class CommandClaimWorldEdit implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        if (GriefPreventionPlugin.instance.worldEditProvider == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandCreateWorldEdit.toText());
            return CommandResult.success();
        }

        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (!playerData.canCreateClaim(player, true)) {
            return CommandResult.success();
        }

        RegionSelector regionSelector = null;
        Region region = null;
        try {
            regionSelector = GriefPreventionPlugin.instance.worldEditProvider.getRegionSelector(player);
            region = regionSelector.getRegion();
        } catch (IncompleteRegionException e) {
            src.sendMessage(Text.of(TextColors.RED, "Could not find a worldedit selection."));
            return CommandResult.success();
        }

        final int minY = playerData.optionClaimCreateMode == 1 ? region.getMinimumPoint().getBlockY() : 0;
        final int maxY = playerData.optionClaimCreateMode == 1 ? region.getMaximumPoint().getBlockY() : 255;
        final Vector3i lesser = new Vector3i(region.getMinimumPoint().getBlockX(), minY, region.getMinimumPoint().getBlockZ());
        final Vector3i greater = new Vector3i(region.getMaximumPoint().getBlockX(), maxY, region.getMaximumPoint().getBlockZ());
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(player);
            final ClaimResult result = GriefPrevention.getApi().createClaimBuilder()
                .bounds(lesser, greater)
                .cuboid(playerData.optionClaimCreateMode == 1)
                .owner(player.getUniqueId())
                .sizeRestrictions(true)
                .type(PlayerUtils.getClaimTypeFromShovel(playerData.shovelMode))
                .world(player.getWorld())
                .build();
            if (result.successful()) {
                final Text message = GriefPreventionPlugin.instance.messageData.claimCreateSuccess
                    .apply(ImmutableMap.of(
                        "type", playerData.shovelMode.name())).build();
                GriefPreventionPlugin.sendMessage(player, message);
            } else {
                if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                    Set<Claim> claims = new HashSet<>();
                    claims.add(result.getClaim().get());
                    CommandHelper.showOverlapClaims(player, claims, 0);
                    GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
                }
            }

        }
        return CommandResult.success();
    }
}
