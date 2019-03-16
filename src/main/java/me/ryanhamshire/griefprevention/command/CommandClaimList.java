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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.util.PaginationUtils;
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
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CommandClaimList implements CommandExecutor {

    private final ClaimType forcedType;
    private boolean canListOthers;
    private boolean canListAdmin;
    private boolean displayOwned = true;
    private final Cache<UUID, String> lastActiveClaimTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public CommandClaimList() {
        this.forcedType = null;
    }

    public CommandClaimList(ClaimType type) {
        this.forcedType = type;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        List<User> userValues = new ArrayList<>(ctx.getAll("user"));
        WorldProperties worldProperties = ctx.<WorldProperties>getOne("world").orElse(null);
        User user = null;
        if (userValues.size() > 0) {
            user = userValues.get(0);
        }

        if (user == null) {
            if (!(src instanceof Player)) {
                GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.commandPlayerInvalid.toText());
                return CommandResult.success();
            }

            user = (User) src;
        }
        if (worldProperties == null) {
            worldProperties = Sponge.getServer().getDefaultWorld().get();
        }

        if (worldProperties == null || !GriefPreventionPlugin.instance.claimsEnabledForWorld(worldProperties)) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.claimDisabledWorld.toText());
            return CommandResult.success();
        }

        // Always reset
        this.displayOwned = true;
        String arguments = "";
        if (user != null) {
            arguments = user.getName();
        }
        if (arguments.isEmpty()) {
            arguments = worldProperties.getWorldName();
        } else {
            arguments += " " + worldProperties.getWorldName();
        }

        this.canListOthers = src.hasPermission(GPPermissions.LIST_OTHER_CLAIMS);
        this.canListAdmin = src.hasPermission(GPPermissions.LIST_OTHER_CLAIMS);
        showClaimList(player, user, this.forcedType, worldProperties);
        return CommandResult.success();
    }

    private void showClaimList(Player src, User user, ClaimType type, WorldProperties worldProperties) {
        List<Text> claimsTextList = new ArrayList<>();
        Set<Claim> claims = new HashSet<>();
        final GPPlayerData sourcePlayerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(worldProperties, src.getUniqueId());
        for (World world : Sponge.getServer().getWorlds()) {
            if (!this.displayOwned && !world.getProperties().getUniqueId().equals(worldProperties.getUniqueId())) {
                continue;
            }

            final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(world.getProperties());
            // load the target player's data
            final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
            Set<Claim> claimList = null;
            int count = 0;
            if (this.displayOwned) {
                claimList = playerData.getInternalClaims();
            } else {
                if (sourcePlayerData.optionRadiusClaimList <= 0) {
                    claimList = claimWorldManager.getInternalWorldClaims();
                } else {
                    claimList = BlockUtils.getNearbyClaims(src.getLocation(), sourcePlayerData.optionRadiusClaimList);
                }
            }
            final int claimListMax = GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().claim.claimListMax;
            for (Claim claim : claimList) {
                // Make sure not to show too many claims or client will time out
                if (!this.displayOwned && claimListMax > 0 && count == claimListMax) {
                    break;
                }
                if (claims.contains(claim)) {
                    continue;
                }

                if (user != null && this.displayOwned) {
                    if (user.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        claims.add(claim);
                        count++;
                    }
                } else if (type != null) {
                    if (claim.getType() == type) {
                        claims.add(claim);
                        count++;
                    }
                } else {
                    claims.add(claim);
                    count++;
                }
            }
        }
        if (src instanceof Player) {
            final Player player = (Player) src;
            final String lastClaimType = this.lastActiveClaimTypeMap.getIfPresent(player.getUniqueId());
            final String currentType = type == null ? "ALL" : type.toString();
            if (lastClaimType != null && !lastClaimType.equals(currentType.toString())) {
                PaginationUtils.resetActivePage(player.getUniqueId());
            }
        }
        claimsTextList = CommandHelper.generateClaimTextList(claimsTextList, claims, worldProperties.getWorldName(), user, src, createClaimListConsumer(src, user, type, worldProperties), this.canListOthers, false);

        final Text whiteOpenBracket = Text.of(TextColors.WHITE, "[");
        final Text whiteCloseBracket = Text.of(TextColors.WHITE, "]");
        Text ownedShowText = Text.of("Click here to view the claims you own.");
        Text allShowText = Text.of("Click here to show all types.");
        Text adminShowText = Text.of("Click here to filter by ", TextColors.RED, "ADMIN ", TextColors.RESET, "type.");
        Text basicShowText = Text.of("Click here to filter by ", TextColors.YELLOW, "BASIC ", TextColors.RESET, "type.");
        Text subdivisionShowText = Text.of("Click here to filter by ", TextColors.AQUA, "SUBDIVISION ", TextColors.RESET, "type.");
        Text townShowText = Text.of("Click here to filter by ", TextColors.GREEN, "TOWN ", TextColors.RESET, "type.");
        Text ownedTypeText = Text.builder()
                .append(Text.of((this.displayOwned && type == null) ? Text.of(whiteOpenBracket, TextColors.GOLD, "OWN", whiteCloseBracket) : Text.of(TextColors.GRAY, "OWN")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, "OWN", worldProperties)))
                .onHover(TextActions.showText(ownedShowText)).build();
        Text allTypeText = Text.builder()
                .append(Text.of((!this.displayOwned && type == null) ? Text.of(whiteOpenBracket, TextColors.LIGHT_PURPLE, "ALL", whiteCloseBracket) : Text.of(TextColors.GRAY, "ALL")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, "ALL", worldProperties)))
                .onHover(TextActions.showText(allShowText)).build();
        Text adminTypeText = Text.builder()
                .append(Text.of(type == ClaimType.ADMIN ? Text.of(whiteOpenBracket, TextColors.RED, "ADMIN", whiteCloseBracket) : Text.of(TextColors.GRAY, "ADMIN")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, ClaimType.ADMIN, worldProperties)))
                .onHover(TextActions.showText(adminShowText)).build();
        Text basicTypeText = Text.builder()
                .append(Text.of(type == ClaimType.BASIC ? Text.of(whiteOpenBracket, TextColors.YELLOW, "BASIC", whiteCloseBracket) : Text.of(TextColors.GRAY, "BASIC")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, ClaimType.BASIC, worldProperties)))
                .onHover(TextActions.showText(basicShowText)).build();
        Text subTypeText = Text.builder()
                .append(Text.of(type == ClaimType.SUBDIVISION ? Text.of(whiteOpenBracket, TextColors.AQUA, "SUBDIVISION", whiteCloseBracket) : Text.of(TextColors.GRAY, "SUBDIVISION")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, ClaimType.SUBDIVISION, worldProperties)))
                .onHover(TextActions.showText(subdivisionShowText)).build();
        Text townTypeText = Text.builder()
                .append(Text.of(type == ClaimType.TOWN ? Text.of(whiteOpenBracket, TextColors.GREEN, "TOWN", whiteCloseBracket) : Text.of(TextColors.GRAY, "TOWN")))
                .onClick(TextActions.executeCallback(createClaimListConsumer(src, user, ClaimType.TOWN, worldProperties)))
                .onHover(TextActions.showText(townShowText)).build();
        Text claimListHead = Text.builder().append(Text.of(
                TextColors.AQUA," Displaying : ", ownedTypeText, "  ", allTypeText, "  ", adminTypeText, "  ", basicTypeText, "  ", subTypeText, "  ", townTypeText)).build();
        final int fillSize = 20 - (claimsTextList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            claimsTextList.add(Text.of(" "));
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(claimListHead).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(claimsTextList);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activePage = PaginationUtils.getActivePage(player.getUniqueId());
            if (activePage == null) {
                activePage = 1;
            }
            this.lastActiveClaimTypeMap.put(player.getUniqueId(), type == null ? "ALL" : type.toString());
        }
        paginationList.sendTo(src, activePage);
    }

    private Consumer<CommandSource> createClaimListConsumer(Player src, User user, String type, WorldProperties worldProperties) {
        return consumer -> {
            if (type.equalsIgnoreCase("ALL")) {
                this.displayOwned = false;
            } else {
                this.displayOwned = true;
            }
            showClaimList(src, user, null, worldProperties);
        };
    }

    private Consumer<CommandSource> createClaimListConsumer(Player src, User user, ClaimType type, WorldProperties worldProperties) {
        return consumer -> {
            this.displayOwned = false;
            showClaimList(src, user, type, worldProperties);
        };
    }
}
