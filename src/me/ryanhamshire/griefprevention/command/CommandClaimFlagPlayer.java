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
import org.spongepowered.api.entity.living.player.User;
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
import java.util.Optional;
import java.util.Set;

public class CommandClaimFlagPlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String name = ctx.<String>getOne("player").get();
        Optional<String> flag = ctx.<String>getOne("flag");
        Optional<String> target = ctx.<String>getOne("target");
        Optional<Tristate> value = ctx.<Tristate>getOne("value");
        Optional<String> context = ctx.<String>getOne("context");

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);
        Optional<User> targetUser = GriefPrevention.instance.resolvePlayerByName(name);
        if (!targetUser.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "The playername " + name + " was not found."));
            return CommandResult.empty();
        } else if (!flag.isPresent() && !value.isPresent()) {
            Set<Context> contextSet = new HashSet<>();
            contextSet.add(claim.getContext());
            List<Object[]> flagList = Lists.newArrayList();
            Map<String, Boolean> permissions = targetUser.get().getSubjectData().getPermissions(contextSet);
            for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                Boolean flagValue = permissionEntry.getValue();
                Object[] flagText = new Object[] { TextColors.GREEN, permissionEntry.getKey().replace(GPPermissions.FLAG_BASE + ".", ""), "  ",
                                TextColors.GOLD, flagValue.toString() };
                flagList.add(flagText);
            }

            List<Text> finalTexts = stripeText(flagList);

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, name + " Flag Permissions")).padding(Text.of("-")).contents(finalTexts);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

        User user = targetUser.get();
        if (user.hasPermission(GPPermissions.COMMAND_IGNORE_CLAIMS) && !src.hasPermission(GPPermissions.SET_ADMIN_FLAGS)) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to change flags on an admin player."));
            return CommandResult.success();
        }

        Subject subj = user.getContainingCollection().get(user.getIdentifier());
        return CommandHelper.addPermission(src, subj, claim, flag.get(), target.get(), value.get(), context, 1);
    }
}
