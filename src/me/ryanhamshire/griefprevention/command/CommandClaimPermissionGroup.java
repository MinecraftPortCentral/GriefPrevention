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

import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandClaimPermissionGroup implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String permission = args.<String>getOne("permission").orElse(null);
        if (permission == null || !player.hasPermission(permission)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You are not allowed to assign a permission that you do not have."));
            return CommandResult.success();
        }
        String group = args.<String>getOne("group").orElse(null);
        String value = args.<String>getOne("value").orElse(null);

        Subject subj = GriefPrevention.instance.permissionService.getGroupSubjects().get(group);
        if (subj == null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid group."));
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        if (claim.isWildernessClaim() && !player.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You must be a wilderness admin to change claim permissions here."));
            return CommandResult.success();
        } else if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to change admin claim permissions."));
            return CommandResult.success();
        }

        Set<Context> contexts = new HashSet<>();
        if (permission == null || value == null) {
            List<Object[]> permList = Lists.newArrayList();
            contexts.add(claim.getContext());
            Map<String, Boolean> permissions = subj.getSubjectData().getPermissions(contexts);
            for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                Boolean permValue = permissionEntry.getValue();
                Object[] permText = new Object[] { TextColors.GREEN, permissionEntry.getKey(), "  ",
                                TextColors.GOLD, permValue.toString() };
                permList.add(permText);
            }

            List<Text> finalTexts = CommandHelper.stripeText(permList);

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, subj.getIdentifier() + " Permissions")).padding(Text.of("-")).contents(finalTexts);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

        Tristate tristateValue = PlayerUtils.getTristateFromString(value);
        if (tristateValue == null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Invalid value entered. '" + value + "' is not a valid value. Valid values are : true, false, undefined, 1, -1, or 0."));
            return CommandResult.success();
        }

        subj.getSubjectData().setPermission(contexts, permission, tristateValue);
        GriefPrevention.sendMessage(src, Text.of("Set permission ", TextColors.AQUA, permission, TextColors.WHITE, " to ", TextColors.GREEN, value, TextColors.WHITE, " on group ", TextColors.GOLD, subj.getIdentifier(), TextColors.WHITE, "."));
        return CommandResult.success();
    }

}
