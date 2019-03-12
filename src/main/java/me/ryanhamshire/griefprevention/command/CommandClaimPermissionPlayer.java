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
import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandClaimPermissionPlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        final String permission = args.<String>getOne("permission").orElse(null);
        if (permission != null && !player.hasPermission(permission)) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionAssignWithoutHaving.toText());
            return CommandResult.success();
        }

        final User user = args.<User>getOne("user").orElse(null);
        final String value = args.<String>getOne("value").orElse(null);
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimManage
                .apply(ImmutableMap.of(
                "type", claim.getType().name())).build();
        if (claim.isWilderness() && !playerData.canManageWilderness) {
            GriefPreventionPlugin.sendMessage(src, message);
            return CommandResult.success();
        } else if (claim.isAdminClaim() && !playerData.canManageAdminClaims) {
            GriefPreventionPlugin.sendMessage(src, message);
            return CommandResult.success();
        }

        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());
        if (permission == null || value == null) {
            // display current permissions for user
            List<Object[]> permList = Lists.newArrayList();
            Map<String, Boolean> permissions = user.getSubjectData().getPermissions(contexts);
            for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                Boolean permValue = permissionEntry.getValue();
                Object[] permText = new Object[] { TextColors.GREEN, permissionEntry.getKey(), "  ",
                                TextColors.GOLD, permValue.toString() };
                permList.add(permText);
            }

            List<Text> finalTexts = CommandHelper.stripeText(permList);

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, user.getName() + " Permissions")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(finalTexts);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

        Tristate tristateValue = PlayerUtils.getTristateFromString(value);
        if (tristateValue == null) {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.RED, "Invalid value entered. '" + value + "' is not a valid value. Valid values are : true, false, undefined, 1, -1, or 0."));
            return CommandResult.success();
        }

        user.getSubjectData().setPermission(contexts, permission, tristateValue);
        GriefPreventionPlugin.sendMessage(src, Text.of("Set permission ", TextColors.AQUA, permission, TextColors.WHITE, " to ", TextColors.GREEN, tristateValue, TextColors.WHITE, " on user ", TextColors.GOLD, user.getName(), TextColors.WHITE, "."));

        return CommandResult.success();
    }

}
