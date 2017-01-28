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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
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
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CommandClaimInfo implements CommandExecutor {

    private static final Text NONE = Text.of(TextColors.GRAY, "none");
    private static final String ADMIN_SETTINGS = "Admin Settings";
    private static final String CLAIM_EXPIRATION = "ClaimExpiration";
    private static final String DENY_MESSAGES = "DenyMessages";
    private static final String FLAG_OVERRIDES = "FlagOverrides";
    private static final String INHERIT_PARENT = "InheritParent";
    private static final String PVP_OVERRIDE = "PvPOverride";
    private static final String REQUIRES_CLAIM_BLOCKS = "RequiresClaimBlocks";

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String claimIdentifier = ctx.<String>getOne("id").orElse(null);
        Player player = null;
        if (src instanceof Player) {
            player = (Player) src;
        }

        if (player == null && claimIdentifier == null) {
            src.sendMessage(Text.of("No valid player or claim id found."));
            return CommandResult.success();
        }

        boolean isAdmin = src.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS);
        Claim claim = null;
        if (player != null && claimIdentifier == null) {
            claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        } else {
            for (WorldProperties worldProperties : Sponge.getServer().getAllWorldProperties()) {
                GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(worldProperties);
                for (Claim worldClaim : claimWorldManager.getWorldClaims()) {
                    if (worldClaim.getUniqueId().toString().equalsIgnoreCase(claimIdentifier)) {
                        claim = worldClaim;
                        break;
                    }
                    Text claimName = worldClaim.getName().orElse(null);
                    if (claimName != null && !claimName.isEmpty()) {
                        if (claimName.toPlain().equalsIgnoreCase(claimIdentifier)) {
                            claim = worldClaim;
                            break;
                        }
                    }
                }
                if (claim != null) {
                    break;
                }
            }
        }

        if (claim == null) {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
            return CommandResult.success();
        }

        GPClaim gpClaim = (GPClaim) claim;
        UUID ownerUniqueId = claim.getOwnerUniqueId();
        if (claim.getParent().isPresent()) {
            ownerUniqueId = claim.getParent().get().getOwnerUniqueId();
        }
        // if not owner of claim, validate perms
        if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            if (!gpClaim.getInternalClaimData().getContainers().contains(player.getUniqueId()) 
                    && !gpClaim.getInternalClaimData().getBuilders().contains(player.getUniqueId())
                    && !gpClaim.getInternalClaimData().getManagers().contains(player.getUniqueId())
                    && !player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_OTHERS)) {
                player.sendMessage(Text.of(TextColors.RED, "You do not have permission to view information in this claim.")); 
                return CommandResult.success();
            }
        }

        User owner = null;
        if (!claim.isWilderness()) {
            owner =  GriefPreventionPlugin.getOrCreateUser(ownerUniqueId);
        }

        List<Text> textList = new ArrayList<>();
        List<Text> adminTextList = new ArrayList<>();
        Text name = claim.getName().orElse(null);
        Text greeting = claim.getData().getGreeting().orElse(null);
        Text farewell = claim.getData().getFarewell().orElse(null);
        String accessors = "";
        String builders = "";
        String containers = "";
        String managers = "";

        double claimY = 65.0D;
        if (gpClaim.isCuboid() || GriefPreventionPlugin.getActiveConfig(gpClaim.world.getProperties()).getConfig().claim.extendIntoGroundDistance != 255) {
            claimY = gpClaim.lesserBoundaryCorner.getY();
        }
        Location<World> southWest = gpClaim.lesserBoundaryCorner.setPosition(new Vector3d(gpClaim.lesserBoundaryCorner.getX(), claimY, gpClaim.greaterBoundaryCorner.getZ()));
        Location<World> northWest = gpClaim.lesserBoundaryCorner.setPosition(new Vector3d(gpClaim.lesserBoundaryCorner.getX(), claimY, gpClaim.lesserBoundaryCorner.getZ()));
        Location<World> southEast = gpClaim.lesserBoundaryCorner.setPosition(new Vector3d(gpClaim.greaterBoundaryCorner.getX(), claimY, gpClaim.greaterBoundaryCorner.getZ()));
        Location<World> northEast = gpClaim.lesserBoundaryCorner.setPosition(new Vector3d(gpClaim.greaterBoundaryCorner.getX(), claimY, gpClaim.lesserBoundaryCorner.getZ()));
        // String southWestCorner = 
        Date created = null;
        Date lastActive = null;
        try {
            Instant instant = claim.getData().getDateCreated();
            created = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        try {
            Instant instant = claim.getData().getDateLastActive();
            lastActive = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        Text claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, name == null ? NONE : name);
        for (UUID uuid : gpClaim.getInternalClaimData().getAccessors()) {
            User user = GriefPreventionPlugin.getOrCreateUser(uuid);
            accessors += user.getName() + " ";
        }
        for (UUID uuid : gpClaim.getInternalClaimData().getBuilders()) {
            User user = GriefPreventionPlugin.getOrCreateUser(uuid);
            builders += user.getName() + " ";
        }
        for (UUID uuid : gpClaim.getInternalClaimData().getContainers()) {
            User user = GriefPreventionPlugin.getOrCreateUser(uuid);
            containers += user.getName() + " ";
        }
        for (UUID uuid : gpClaim.getInternalClaimData().getManagers()) {
            User user = GriefPreventionPlugin.getOrCreateUser(uuid);
            managers += user.getName() + " ";
        }

        final Text adminClaimText = Text.of(TextColors.RED, "ADMIN");
        final Text basicClaimText = Text.of(TextColors.GREEN, "BASIC");
        TextColor claimTypeColor = TextColors.GREEN;
        if (claim.isAdminClaim()) {
            if (claim.isSubdivision()) {
                claimTypeColor = TextColors.DARK_AQUA;
            } else {
                claimTypeColor = TextColors.RED;
            }
        } else if (claim.isSubdivision()) {
            claimTypeColor = TextColors.AQUA;
        }
        
        if (isAdmin) {
            Text returnToClaimInfo = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to standard settings", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", ""))).build();
            Text claimDenyMessages = Text.of(TextColors.YELLOW, DENY_MESSAGES, TextColors.WHITE, " : ", getClickableInfoText(src, claim, DENY_MESSAGES, gpClaim.getInternalClaimData().allowDenyMessages() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")), TextColors.RESET);
            Text claimRequiresClaimBlocks = Text.of(TextColors.YELLOW, REQUIRES_CLAIM_BLOCKS, TextColors.WHITE, " : ", getClickableInfoText(src, claim, REQUIRES_CLAIM_BLOCKS, gpClaim.getInternalClaimData().requiresClaimBlocks() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")), TextColors.RESET);
            Text claimExpiration = Text.of(TextColors.YELLOW, CLAIM_EXPIRATION, TextColors.WHITE, " : ", getClickableInfoText(src, claim, CLAIM_EXPIRATION, gpClaim.getInternalClaimData().allowClaimExpiration() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")), TextColors.RESET);
            Text claimFlagOverrides = Text.of(TextColors.YELLOW, FLAG_OVERRIDES, TextColors.WHITE, " : ", getClickableInfoText(src, claim, FLAG_OVERRIDES, gpClaim.getInternalClaimData().allowFlagOverrides() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")), TextColors.RESET);
            Text pvp = Text.of(TextColors.YELLOW, "PvP", TextColors.WHITE, " : ", getClickableInfoText(src, claim, PVP_OVERRIDE, gpClaim.getInternalClaimData().getPvpOverride() == Tristate.TRUE ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, gpClaim.getInternalClaimData().getPvpOverride().name())), TextColors.RESET);
            adminTextList.add(returnToClaimInfo);
            adminTextList.add(claimDenyMessages);
            if (!claim.isAdminClaim() && !claim.isWilderness()) {
                adminTextList.add(claimRequiresClaimBlocks);
                adminTextList.add(claimExpiration);
            }
            adminTextList.add(claimFlagOverrides);
            adminTextList.add(pvp);
            Text adminSettings = Text.builder()
                    .append(Text.of(TextStyles.ITALIC, TextColors.RED, ADMIN_SETTINGS))
                    .onClick(TextActions.executeCallback(createAdminSettingsConsumer(src, claim, adminTextList)))
                    .onHover(TextActions.showText(Text.of("Click here to view admin settings")))
                    .build();
            textList.add(adminSettings);
        }

        Text claimId = Text.join(Text.of(TextColors.YELLOW, "UUID", TextColors.WHITE, " : ",
                Text.builder()
                        .append(Text.of(TextColors.GRAY, claim.getUniqueId().toString()))
                        .onShiftClick(TextActions.insertText(claim.getUniqueId().toString())).build()));
        Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GOLD, owner != null && !claim.isAdminClaim() ? owner.getName() : "administrator");
        Text claimType = Text.builder()
                .append(Text.of(claimTypeColor, claim.getType().name()))
                .onClick(TextActions.executeCallback(createClaimTypeConsumer(src, claim, isAdmin)))
                .onHover(TextActions.showText(Text.of("Click here to switch claim type to ", claim.isAdminClaim() ? basicClaimText : adminClaimText)))
                .build();
        Text claimTypeInfo = Text.of(TextColors.YELLOW, "Type", TextColors.WHITE, " : ", 
                claimType, " ", TextColors.GRAY, claim.isCuboid() ? "3D " : "2D ",
                TextColors.WHITE, " (Area: ", TextColors.GRAY, claim.getArea(), " blocks",
                TextColors.WHITE, ")");
        Text claimInherit = Text.of(TextColors.YELLOW, INHERIT_PARENT, TextColors.WHITE, " : ", getClickableInfoText(src, claim, INHERIT_PARENT, claim.getData().doesInheritParent() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")), TextColors.RESET);
        Text claimFarewell = Text.of(TextColors.YELLOW, "Farewell", TextColors.WHITE, " : ", TextColors.RESET,
                farewell == null ? NONE : farewell);
        Text claimGreeting = Text.of(TextColors.YELLOW, "Greeting", TextColors.WHITE, " : ", TextColors.RESET,
                greeting == null ? NONE : greeting);
        Text claimSpawn = null;
        if (claim.getData().getSpawnPos().isPresent()) {
            Vector3i spawnPos = claim.getData().getSpawnPos().get();
            Location<World> spawnLoc = new Location<>(player.getWorld(), spawnPos);
            claimSpawn = Text.builder().append(Text.of(TextColors.GREEN, "Spawn", TextColors.WHITE, " : ", TextColors.GRAY, spawnPos))
                    .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, spawnLoc, claim)))
                    .onHover(TextActions.showText(Text.of("Click here to teleport to claim spawn.")))
                    .build();
        }
        Text southWestCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "SW", TextColors.WHITE, " : ", TextColors.GRAY, southWest.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, southWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to SW corner of claim.")))
                .build();
        Text southEastCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "SE", TextColors.WHITE, " : ", TextColors.GRAY, southEast.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, southEast, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to SE corner of claim.")))
                .build();
        Text southCorners = Text.builder()
                .append(Text.of(TextColors.YELLOW, "SouthCorners", TextColors.WHITE, " : "))
                .append(southWestCorner)
                .append(southEastCorner).build();
        Text northWestCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "NW", TextColors.WHITE, " : ", TextColors.GRAY, northWest.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, northWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to NW corner of claim.")))
                .build();
        Text northEastCorner = Text.builder()
                .append(Text.of(TextColors.LIGHT_PURPLE, "NE", TextColors.WHITE, " : ", TextColors.GRAY, northEast.getBlockPosition(), " "))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(player, northEast, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to NE corner of claim.")))
                .build();
        Text northCorners = Text.builder()
                .append(Text.of(TextColors.YELLOW, "NorthCorners", TextColors.WHITE, " : "))
                .append(northWestCorner)
                .append(northEastCorner).build();
        Text claimAccessors = Text.of(TextColors.YELLOW, "Accessors", TextColors.WHITE, " : ", TextColors.BLUE, accessors.equals("") ? NONE : accessors);
        Text claimBuilders = Text.of(TextColors.YELLOW, "Builders", TextColors.WHITE, " : ", TextColors.YELLOW, builders.equals("") ? NONE : builders);
        Text claimContainers = Text.of(TextColors.YELLOW, "Containers", TextColors.WHITE, " : ", TextColors.GREEN, containers.equals("") ? NONE : containers);
        Text claimCoowners = Text.of(TextColors.YELLOW, "Managers", TextColors.WHITE, " : ", TextColors.GOLD, managers.equals("") ? NONE : managers);
        Text dateCreated = Text.of(TextColors.YELLOW, "Created", TextColors.WHITE, " : ", TextColors.GRAY, created != null ? created : "Unknown");
        Text dateLastActive = Text.of(TextColors.YELLOW, "LastActive", TextColors.WHITE, " : ", TextColors.GRAY, lastActive != null ? lastActive : "Unknown");
        Text worldName = Text.of(TextColors.YELLOW, "World", TextColors.WHITE, " : ", TextColors.GRAY, claim.getWorld().getProperties().getWorldName());

        if (claimSpawn != null) {
            textList.add(claimSpawn);
        }
        textList.add(claimName);
        textList.add(ownerLine);
        textList.add(claimTypeInfo);
        textList.add(claimInherit);
        textList.add(claimAccessors);
        textList.add(claimBuilders);
        textList.add(claimContainers);
        textList.add(claimCoowners);
        textList.add(claimGreeting);
        textList.add(claimFarewell);
        textList.add(worldName);
        textList.add(dateCreated);
        textList.add(dateLastActive);
        textList.add(claimId);
        textList.add(northCorners);
        textList.add(southCorners);
        if (!claim.getParent().isPresent()) {
            textList.remove(claimInherit);
        }
        if (claim.isWilderness()) {
            textList.remove(claimAccessors);
            textList.remove(claimBuilders);
            textList.remove(claimContainers);
            textList.remove(claimCoowners);
            textList.remove(dateLastActive);
            textList.remove(northCorners);
            textList.remove(southCorners);
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA, "Claim Info")).padding(Text.of("-")).contents(textList);
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }

    public static Consumer<CommandSource> createAdminSettingsConsumer(CommandSource src, Claim claim, List<Text> adminTextList) {
        return admin -> {
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, "Admin Claim Info")).padding(Text.of("-")).contents(adminTextList);
            paginationBuilder.sendTo(src);
        };
    }

    public static Text getClickableInfoText(CommandSource src, Claim claim, String title, Text infoText) {
        String onClickText = "Click here to toggle value.";
        boolean hasPermission = true;
        if (src instanceof Player) {
            String denyReason = ((GPClaim) claim).allowEdit((Player) src);
            if (denyReason != null) {
                onClickText = denyReason;
                hasPermission = false;
            }
        }

        Text.Builder textBuilder = Text.builder()
                .append(infoText)
                .onHover(TextActions.showText(Text.of(onClickText)));
        if (hasPermission) {
            textBuilder.onClick(TextActions.executeCallback(createClaimInfoConsumer(src, claim, title)));
        }
        return textBuilder.build();
    }

    private static Consumer<CommandSource> createClaimInfoConsumer(CommandSource src, Claim claim, String title) {
        GPClaim gpClaim = (GPClaim) claim;
        return info -> {
            switch (title) {
                case INHERIT_PARENT : 
                    if (!claim.getParent().isPresent() || !src.hasPermission(GPPermissions.COMMAND_SUBDIVISION_INHERIT)) {
                        return;
                    }

                    gpClaim.getInternalClaimData().setInheritParent(!gpClaim.getInternalClaimData().doesInheritParent());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    claim.getData().save();

                    if (!gpClaim.getData().doesInheritParent()) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "InheritParent ", TextColors.RED, "OFF"));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "InheritParent ", TextColors.GREEN, "ON"));
                    }
                    break;
                case CLAIM_EXPIRATION :
                    gpClaim.getInternalClaimData().setClaimExpiration(!gpClaim.getInternalClaimData().allowClaimExpiration());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();

                    if (!gpClaim.getInternalClaimData().allowClaimExpiration()) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "ClaimExpiration ", TextColors.RED, "OFF"));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "ClaimExpiration ", TextColors.GREEN, "ON"));
                    }
                    break;
                case DENY_MESSAGES :
                    gpClaim.getInternalClaimData().setDenyMessages(!gpClaim.getInternalClaimData().allowDenyMessages());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();

                    if (!gpClaim.getInternalClaimData().allowDenyMessages()) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "DenyMessages ", TextColors.RED, "OFF"));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "DenyMessages ", TextColors.GREEN, "ON"));
                    }
                    break;
                case FLAG_OVERRIDES :
                    gpClaim.getInternalClaimData().setFlagOverrides(!gpClaim.getInternalClaimData().allowFlagOverrides());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();

                    if (!gpClaim.getInternalClaimData().allowFlagOverrides()) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "FlagOverride ", TextColors.RED, "OFF"));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "FlagOverride ", TextColors.GREEN, "ON"));
                    }
                    break;
                case PVP_OVERRIDE :
                    Tristate value = gpClaim.getInternalClaimData().getPvpOverride();
                    Text newValue = Text.of();
                    if (value == Tristate.UNDEFINED) {
                        newValue = Text.of(TextColors.GREEN, Tristate.TRUE);
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.TRUE);
                    } else if (value == Tristate.TRUE) {
                        newValue = Text.of(TextColors.RED, Tristate.FALSE);
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.FALSE);
                    } else {
                        newValue = Text.of(TextColors.GRAY, Tristate.UNDEFINED);
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.UNDEFINED);
                    }
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();

                    GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "PvPOverride ", newValue));
                    break;
                case REQUIRES_CLAIM_BLOCKS :
                    boolean requiresClaimBlocks = gpClaim.getInternalClaimData().requiresClaimBlocks();
                    gpClaim.getInternalClaimData().setRequiresClaimBlocks(!requiresClaimBlocks);
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();

                    if (requiresClaimBlocks) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "RequiresClaimBlocks ", TextColors.RED, "OFF"));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextColors.WHITE, "RequiresClaimBlocks ", TextColors.GREEN, "ON"));
                    }
                    break;
                default:
            }
        };
    }

    private static Consumer<CommandSource> createClaimTypeConsumer(CommandSource src, Claim gpClaim, boolean isAdmin) {
        GPClaim claim = (GPClaim) gpClaim;
        return type -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }
            if (!isAdmin) {
                src.sendMessage(Text.of(TextColors.RED, "You do not have permission to change the type of this claim."));
                return;
            }
            if (claim.isWilderness()) {
                src.sendMessage(Text.of(TextColors.RED, "The wilderness cannot be changed."));
                return;
            }
            if (claim.parent != null) {
                src.sendMessage(Text.of(TextColors.RED, "Subdivisions cannot be changed."));
                return;
            }

            Player player = (Player) src;
            if (claim.isBasicClaim()) {
                GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(claim.world.getProperties());
                List<Claim> playerClaims = claimWorldManager.getInternalPlayerClaims(player.getUniqueId());
                if (playerClaims != null) {
                    playerClaims.remove(claim);
                }

                GPPlayerData playerData = claimWorldManager.getOrCreatePlayerData(player.getUniqueId());
                playerData.revertActiveVisual(player);
                claim.visualization = null;
                claim.setOwnerUniqueId(null);
                claim.type = ClaimType.ADMIN;
                claim.getInternalClaimData().setType(ClaimType.ADMIN);
                src.sendMessage(Text.of(TextColors.GREEN, "Successfully changed claim type to ", TextColors.RED, "ADMIN", TextColors.GREEN, "." ));
            } else {
                GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(claim.world.getProperties());
                List<Claim> playerClaims = claimWorldManager.getInternalPlayerClaims(player.getUniqueId());
                if (playerClaims != null && !playerClaims.contains(claim)) {
                    playerClaims.add(claim);
                }
                GPPlayerData playerData = claimWorldManager.getOrCreatePlayerData(player.getUniqueId());
                playerData.revertActiveVisual(player);
                claim.type = ClaimType.BASIC;
                claim.setOwnerUniqueId(player.getUniqueId());
                claim.visualization = null;
                claim.getInternalClaimData().setOwnerUniqueId(player.getUniqueId());
                claim.getInternalClaimData().setType(ClaimType.BASIC);
                src.sendMessage(Text.of(TextColors.GREEN, "Successfully changed claim type to ", TextColors.AQUA, "BASIC", TextColors.GREEN, "." ));
            }
            // revert visuals for all players watching this claim
            List<UUID> playersWatching = new ArrayList<>(claim.playersWatching);
            for (UUID playerUniqueId : playersWatching) {
                player = Sponge.getServer().getPlayer(playerUniqueId).orElse(null);
                if (player != null) {
                    GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(claim.world, playerUniqueId);
                    playerData.revertActiveVisual(player);
                }
            }
            claim.getClaimStorage().save();
        };
    }
}
