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

import static me.ryanhamshire.griefprevention.command.CommandClaimFlag.stripeText;

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
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CommandClaimFlagGroup implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String group = ctx.<String>getOne("group").get();
        Optional<String> flag = ctx.<String>getOne("flag");
        Optional<String> target = ctx.<String>getOne("target");
        Optional<Tristate> value = ctx.<Tristate>getOne("value");
        Optional<String> context = ctx.<String>getOne("context");

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);
        if (claim == null) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found."));
            return CommandResult.success();
        } else if (!flag.isPresent() && !value.isPresent()) {
            Set<Context> contextSet = new HashSet<>();
            contextSet.add(claim.getContext());
            PermissionService service = Sponge.getServiceManager().provide(PermissionService.class).get();
            Subject subj = service.getGroupSubjects().get(group);
            if (subj != null) {
                List<Object[]> flagList = Lists.newArrayList();
                Map<String, Boolean> permissions = subj.getSubjectData().getPermissions(contextSet);
                for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                    Boolean flagValue = permissionEntry.getValue();
                    Object[] flagText = new Object[] { TextColors.GREEN, permissionEntry.getKey().replace(GPPermissions.FLAG_BASE + ".", ""), "  ",
                                    TextColors.GOLD, flagValue.toString() };
                    flagList.add(flagText);
                }

                List<Text> finalTexts = stripeText(flagList);

                PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                PaginationList.Builder paginationBuilder = paginationService.builder()
                        .title(Text.of(TextColors.AQUA, group + " Flag Permissions")).padding(Text.of("-")).contents(finalTexts);
                paginationBuilder.sendTo(src);
            }
            return CommandResult.success();
        }

        Subject subj = GriefPrevention.instance.permissionService.getGroupSubjects().get(group);
        if (subj == null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Not a valid group."));
            return CommandResult.success();
        }
        return CommandHelper.addPermission(src, subj, claim, flag.get(), target.get(), value.get(), context, 2);
    }


}
