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

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CommandTrustList implements CommandExecutor {

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

        // if no claim here, error message
        if (claim == null) {
            try {
                throw new CommandException(GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        showTrustList(src, claim, player, TrustType.NONE);
        return CommandResult.success();

    }

    public static void showTrustList(CommandSource src, GPClaim claim, Player player, TrustType type) {
        final Text whiteOpenBracket = Text.of(TextColors.AQUA, "[");
        final Text whiteCloseBracket = Text.of(TextColors.AQUA, "]");
        final Text showAllText = Text.of("Click here to show all trusted users for claim.");
        final Text showAccessorText = Text.of("Click here to filter by ", TextColors.YELLOW, "ACCESSOR ", TextColors.RESET, "permissions.");
        final Text showContainerText = Text.of("Click here to filter by ", TextColors.LIGHT_PURPLE, "CONTAINER ", TextColors.RESET, "permissions.");
        final Text showBuilderText = Text.of("Click here to filter by ", TextColors.GREEN, "BUILDER ", TextColors.RESET, "permissions.");
        final Text showManagerText = Text.of("Click here to filter by ", TextColors.GOLD, "MANAGER ", TextColors.RESET, "permissions.");
        final Text allTypeText = Text.builder()
                .append(Text.of(type == TrustType.NONE ? Text.of(whiteOpenBracket, TextColors.WHITE, "ALL", whiteCloseBracket) : Text.of(TextColors.GRAY, "ALL")))
                .onClick(TextActions.executeCallback(createTrustConsumer(src, claim, player, TrustType.NONE)))
                .onHover(TextActions.showText(showAllText)).build();
        final Text accessorTrustText = Text.builder()
                .append(Text.of(type == TrustType.ACCESSOR ? Text.of(whiteOpenBracket, TextColors.YELLOW, "ACCESSOR", whiteCloseBracket) : Text.of(TextColors.GRAY, "ACCESSOR")))
                .onClick(TextActions.executeCallback(createTrustConsumer(src, claim, player, TrustType.ACCESSOR)))
                .onHover(TextActions.showText(showAccessorText)).build();
        final Text builderTrustText = Text.builder()
                .append(Text.of(type == TrustType.BUILDER ? Text.of(whiteOpenBracket, TextColors.GREEN, "BUILDER", whiteCloseBracket) : Text.of(TextColors.GRAY, "BUILDER")))
                .onClick(TextActions.executeCallback(createTrustConsumer(src, claim, player, TrustType.BUILDER)))
                .onHover(TextActions.showText(showBuilderText)).build();
        final Text containerTrustText = Text.builder()
                .append(Text.of(type == TrustType.CONTAINER ? Text.of(whiteOpenBracket, TextColors.LIGHT_PURPLE, "CONTAINER", whiteCloseBracket) : Text.of(TextColors.GRAY, "CONTAINER")))
                .onClick(TextActions.executeCallback(createTrustConsumer(src, claim, player, TrustType.CONTAINER)))
                .onHover(TextActions.showText(showContainerText)).build();
        final Text managerTrustText = Text.builder()
                .append(Text.of(type == TrustType.MANAGER ? Text.of(whiteOpenBracket, TextColors.GOLD, "MANAGER", whiteCloseBracket) : Text.of(TextColors.GRAY, "MANAGER")))
                .onClick(TextActions.executeCallback(createTrustConsumer(src, claim, player, TrustType.MANAGER)))
                .onHover(TextActions.showText(showManagerText)).build();
        final Text claimTrustHead = Text.builder().append(Text.of(
                TextColors.AQUA," Displaying : ", allTypeText, "  ", accessorTrustText, "  ", builderTrustText, "  ", containerTrustText, "  ", managerTrustText)).build();

        List<UUID> userIdList = new ArrayList<>(claim.getUserTrusts());
        List<Text> trustList = new ArrayList<>();
        trustList.add(Text.of(""));

        if (type == TrustType.NONE) {
            // check highest trust first
            for (UUID uuid : claim.getInternalClaimData().getManagers()) {
                final User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                trustList.add(Text.of(TextColors.GOLD, user.getName()));
                userIdList.remove(user.getUniqueId());
            }

            for (UUID uuid : claim.getInternalClaimData().getBuilders()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                trustList.add(Text.of(TextColors.GREEN, user.getName()));
                userIdList.remove(uuid);
            }
    
            /*for (String group : claim.getInternalClaimData().getManagerGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
            for (UUID uuid : claim.getInternalClaimData().getContainers()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                trustList.add(Text.of(TextColors.LIGHT_PURPLE, user.getName()));
                userIdList.remove(uuid);
            }
    
           /* for (String group : claim.getInternalClaimData().getBuilderGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
            for (UUID uuid : claim.getInternalClaimData().getAccessors()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                trustList.add(Text.of(TextColors.YELLOW, user.getName()));
                userIdList.remove(uuid);
            }
    
            /*for (String group : claim.getInternalClaimData().getContainerGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }
    
            player.sendMessage(permissions.build());
            permissions = Text.builder(">").color(TextColors.BLUE);
    
            for (UUID uuid : claim.getInternalClaimData().getAccessors()) {
                User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }
    
            for (String group : claim.getInternalClaimData().getAccessorGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
        } else {
            for (UUID uuid : claim.getUserTrusts(type)) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                User user = GriefPreventionPlugin.getOrCreateUser(uuid);
                trustList.add(Text.of(getTrustColor(type), user.getName()));
                userIdList.remove(uuid);
            }
        }

        int fillSize = 20 - (trustList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            trustList.add(Text.of(" "));
        }
        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(claimTrustHead).padding(Text.of(TextStyles.STRIKETHROUGH,"-")).contents(trustList);
        paginationBuilder.sendTo(src);

    }

    private static TextColor getTrustColor(TrustType type) {
        if (type == TrustType.NONE) {
            return TextColors.WHITE;
        }
        if (type == TrustType.ACCESSOR) {
            return TextColors.YELLOW;
        }
        if (type == TrustType.BUILDER) {
            return TextColors.GREEN;
        }
        if (type == TrustType.CONTAINER) {
            return TextColors.LIGHT_PURPLE;
        }
        return TextColors.GOLD;
    }

    private static Consumer<CommandSource> createTrustConsumer(CommandSource src, GPClaim claim, Player player, TrustType type) {
        return consumer -> {
            showTrustList(src, claim, player, type);
        };
    }
}
