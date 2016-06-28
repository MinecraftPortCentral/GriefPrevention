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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class CommandClaimFlag implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Optional<String> flag = ctx.<String>getOne("flag");
        Optional<String> target = ctx.<String>getOne("target");
        Optional<Tristate> value = ctx.<Tristate>getOne("value");
        Optional<String> context = ctx.<String>getOne("context");

        Player player;

        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);

        if (claim != null) {
            if (!flag.isPresent() && !value.isPresent() && src.hasPermission(GPPermissions.COMMAND_LIST_CLAIM_FLAGS)) {
                List<Object[]> flagList = Lists.newArrayList();
                Set<Context> contexts = new HashSet<>();
                Set<Context> overrideContexts = new HashSet<>();
                if (claim.isBasicClaim() || claim.isSubdivision()) {
                    contexts.add(GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
                    overrideContexts.add(GriefPrevention.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT);
                } else if (claim.isAdminClaim()) {
                    contexts.add(GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
                    overrideContexts.add(GriefPrevention.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT);
                } else {
                    contexts.add(GriefPrevention.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
                }
                contexts.add(claim.world.getContext());
                if (!overrideContexts.isEmpty()) {
                    overrideContexts.add(claim.world.getContext());
                }
                Map<String, Boolean> defaultPermissions = GriefPrevention.GLOBAL_SUBJECT.getSubjectData().getPermissions(contexts);
                Map<String, Boolean> overridePermissions = GriefPrevention.GLOBAL_SUBJECT.getSubjectData().getPermissions(overrideContexts);
                Map<String, Boolean> claimPermissions = GriefPrevention.GLOBAL_SUBJECT.getSubjectData().getPermissions(ImmutableSet.of(claim.context));
                for (Map.Entry<String, Boolean> overridePermissionEntry : overridePermissions.entrySet()) {
                    Boolean flagValue = overridePermissionEntry.getValue();
                    Object[] flagText = new Object[] { TextColors.GREEN, overridePermissionEntry.getKey().replace(GPPermissions.FLAG_BASE + ".", ""), "  ",
                                    TextColors.RED, flagValue.toString() };
                    flagList.add(flagText);
                }

                for (Map.Entry<String, Boolean> defaultPermissionEntry : defaultPermissions.entrySet()) {
                    if (!claimPermissions.containsKey(defaultPermissionEntry.getKey()) && !overridePermissions.containsKey(defaultPermissionEntry.getKey())) {
                        Boolean flagValue = defaultPermissionEntry.getValue();
                        Object[] flagText = new Object[] { TextColors.GREEN, defaultPermissionEntry.getKey().replace(GPPermissions.FLAG_BASE + ".", ""), "  ",
                                        TextColors.LIGHT_PURPLE, flagValue.toString() };
                        flagList.add(flagText);
                    }
                }

                for (Map.Entry<String, Boolean> permissionEntry : claimPermissions.entrySet()) {
                    if (!overridePermissions.containsKey(permissionEntry.getKey())) {
                        Boolean flagValue = permissionEntry.getValue();
                        Object[] flagText = new Object[] { TextColors.GREEN, permissionEntry.getKey().replace(GPPermissions.FLAG_BASE + ".", ""), "  ",
                                        TextColors.GOLD, flagValue.toString() };
                        flagList.add(flagText);
                    }
                }

                Collections.sort(flagList, (t1, t2) -> Text.of(t1).compareTo(Text.of(t2)));
                List<Text> finalTexts = this.stripeText(flagList);

                PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                PaginationList.Builder paginationBuilder = paginationService.builder()
                        .title(Text.of(TextColors.AQUA, "Claim Flag Permissions")).padding(Text.of("-")).contents(finalTexts);
                paginationBuilder.sendTo(src);
                return CommandResult.success();
            } else if (flag.isPresent() && value.isPresent()) {
                if (GPFlags.DEFAULT_FLAGS.containsKey(flag.get())) {
                    CommandHelper.addPermission(src, GriefPrevention.GLOBAL_SUBJECT, claim, flag.get(), target.get(), value.get(), context, 0);
                } else {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Invalid flag entered."));
                }
                return CommandResult.success();
            }

            GriefPrevention.sendMessage(src, CommandMessageFormatting.error(Text.of("Usage: /cf [<flag> <target> <value> [subject|context]]")));
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim found."));
        }
        return CommandResult.success();
    }

    private List<Text> stripeText(List<Object[]> texts) {
        ImmutableList.Builder<Text> finalTexts = ImmutableList.builder();
        for (int i = 0; i < texts.size(); i++) {
            Object[] text = texts.get(i);
            text[0] = i % 2 == 0 ? TextColors.GREEN : TextColors.AQUA; // Set starting color
            finalTexts.add(Text.of(text));
        }
        return finalTexts.build();

        // TODO - figure out alignment taking into account variable character width

        /*List<List<Object[]>> pages = Lists.partition(texts, 20); // Page size of 20
        int[] maximums = new int[pages.size()];

        for (int i = 0; i < pages.size(); i++) {
            List<Object[]> page = pages.get(i);

            int max = 0;
            for (Object[] text: page) {
                int len = ((String) text[1]).length();
                if (len > max) {
                    max = len;
                }
            }
            maximums[i] = max;
        }*/

        /*ImmutableList.Builder<Text> finalTexts = ImmutableList.builder();
        for (int i = 0; i < pages.size(); i++) {
            for (Object[] text: pages.get(i)) {
                text[0] = i % 2 == 0 ? TextColors.GREEN : TextColors.AQUA; // Set starting color
                text[2] = Strings.repeat(" ", Math.max(2, (maximums[i] - ((String) text[1]).length()))); // Set number of spaces to page maximum minus flag (text[1]) length

                finalTexts.add(Text.of(text));
            }
        }*/
    }
}
