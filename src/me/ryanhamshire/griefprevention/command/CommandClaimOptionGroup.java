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
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
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
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandClaimOptionGroup implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String option = args.<String>getOne("option").orElse(null);
        if (!player.hasPermission(GPPermissions.MANAGE_PERMISSION_OPTIONS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You are not allowed to assign an option to groups."));
            return CommandResult.success();
        }
        String group = args.<String>getOne("group").orElse(null);
        String value = args.<String>getOne("value").orElse(null);

        Subject subj = GriefPrevention.instance.permissionService.getGroupSubjects().get(group);
        if (subj == null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid group."));
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);
        if (claim.isWildernessClaim() && !player.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You must be a wilderness admin to change claim options here."));
            return CommandResult.success();
        } else if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to change admin claim options."));
            return CommandResult.success();
        }

        final OptionSubjectData subjectData = ((OptionSubjectData) subj.getSubjectData());
        Set<Context> contexts = new HashSet<>();
        if (option == null || value == null) {
            List<Object[]> optionList = Lists.newArrayList();
            contexts.add(claim.getContext());
            Map<String, String> options = subjectData.getOptions(contexts);
            for (Map.Entry<String, String> optionEntry : options.entrySet()) {
                String optionValue = optionEntry.getValue();
                Object[] optionText = new Object[] { TextColors.GREEN, optionEntry.getKey(), "  ",
                                TextColors.GOLD, optionValue };
                optionList.add(optionText);
            }

            List<Text> finalTexts = CommandHelper.stripeText(optionList);

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, subj.getIdentifier() + " Options")).padding(Text.of("-")).contents(finalTexts);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

        if (subjectData.setOption(contexts, option, value)) {
            GriefPrevention.sendMessage(src, Text.of("Set option ", TextColors.AQUA, option, TextColors.WHITE, " to ", TextColors.GREEN, value, TextColors.WHITE, " on group ", TextColors.GOLD, subj.getIdentifier(), TextColors.WHITE, "."));
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "The permission plugin failed to set the option."));
        }
        return CommandResult.success();
    }

}
