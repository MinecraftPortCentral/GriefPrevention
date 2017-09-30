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
package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.text.Text;

import java.util.Set;

public class CommandClaimFlagReset implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimResetFlags
                .apply(ImmutableMap.of(
                "type", claim.getType().name())).build();
        if (claim.isWilderness()) {
            if (!src.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
                GriefPreventionPlugin.sendMessage(src, message);
                return CommandResult.success();
            }
        } else if (claim.isAdminClaim()) {
            if (!player.getUniqueId().equals(claim.getOwnerUniqueId()) && !src.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                GriefPreventionPlugin.sendMessage(src, message);
                return CommandResult.success();
            }
        } else if (!src.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && (claim.isBasicClaim() || claim.isSubdivision()) && !player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionClaimResetFlagsSelf.toText());
            return CommandResult.success();
        }

        // Remove persisted data
        for (Set<Context> contextSet : GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getAllPermissions().keySet()) {
            if (contextSet.contains(claim.getContext())) {
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(contextSet);
            }
        }
        for (Set<Context> contextSet : GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getAllOptions().keySet()) {
            if (contextSet.contains(claim.getContext())) {
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(contextSet);
            }
        }
        for (Set<Context> contextSet : GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getAllParents().keySet()) {
            if (contextSet.contains(claim.getContext())) {
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(contextSet);
            }
        }

        GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.flagResetSuccess.toText());
        return CommandResult.success();
    }
}
