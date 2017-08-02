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
import me.ryanhamshire.griefprevention.permission.GPPermissions;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandClaimOptionPlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String option = args.<String>getOne("option").orElse(null);
        if (option != null && !option.startsWith("griefprevention.")) {
            option = "griefprevention." + option;
        }
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);

        if (claim.isSubdivision()) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.commandOptionInvalidClaim.toText());
            return CommandResult.success();
        }
        if (!playerData.canManageOption(player, claim, false)) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionPlayerOption.toText());
            return CommandResult.success();
        }

        User user = args.<User>getOne("user").orElse(null);
        Double value = args.<Double>getOne("value").orElse(null);
        final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimManage
                .apply(ImmutableMap.of(
                "type", claim.getType().name())).build();
        if (claim.isWilderness() && !player.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
            GriefPreventionPlugin.sendMessage(src, message);
            return CommandResult.success();
        } else if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
            GriefPreventionPlugin.sendMessage(src, message);
            return CommandResult.success();
        }

        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());
        if (option == null || value == null) {
            // display current options for user
            List<Object[]> optionList = Lists.newArrayList();
            Map<String, String> options = user.getSubjectData().getOptions(contexts);
            for (Map.Entry<String, String> optionEntry : options.entrySet()) {
                String optionValue = optionEntry.getValue();
                Object[] optionText = new Object[] { TextColors.GREEN, optionEntry.getKey(), "  ",
                                TextColors.GOLD, optionValue };
                optionList.add(optionText);
            }

            List<Text> finalTexts = CommandHelper.stripeText(optionList);

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, user.getName() + " Claim Options")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(finalTexts);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

        if (user.getSubjectData().setOption(contexts, option, value.toString())) {
            GriefPreventionPlugin.sendMessage(src, Text.of("Set option ", TextColors.AQUA, option, TextColors.WHITE, " to ", TextColors.GREEN, value, TextColors.WHITE, " on user ", TextColors.GOLD, user.getName(), TextColors.WHITE, "."));
        } else {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.RED, "The permission plugin failed to set the option."));
        }

        return CommandResult.success();
    }

}
