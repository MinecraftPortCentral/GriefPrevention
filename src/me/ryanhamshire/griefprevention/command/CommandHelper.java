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

import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimPermission;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CommandHelper {

    public static Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Text.of("You must be a player to run this command!"));
        }
    }

    public static CommandResult abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        // which claim is being abandoned?
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, playerData.lastClaim);
        if (claim.isWildernessClaim()) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
        }

        // verify ownership
        else if (claim.allowEdit(player) != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
        }

        // warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        } else {
            // delete it
            claim.removeSurfaceFluids(null);
            GriefPrevention.instance.dataStore.deleteClaim(claim, true);

            // if in a creative mode world, restore the claim area
            if (GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.addLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // adjust claim blocks when abandoning a top level claim
            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                playerData.setAccruedClaimBlocks(
                        playerData.getAccruedClaimBlocks() - (int) Math
                                .ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties())
                                        .getConfig().claim.abandonReturnRatio))));

                // tell the player how many claim blocks he has left
                int remainingBlocks = playerData.getRemainingClaimBlocks();
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));
            }

            // revert any current visualization
            Visualization.Revert(player);

            playerData.warnedAboutMajorDeletion = false;
        }

        return CommandResult.success();

    }

    // helper method to resolve a player name from the player's UUID
    public static String lookupPlayerName(UUID playerID) {
        // parameter validation
        if (playerID == null) {
            return "somebody";
        }

        // check the cache
        Optional<User> player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (player.isPresent()) {
            return player.get().getName();
        } else {
            try {
                return Sponge.getServer().getGameProfileManager().get(playerID).get().getName().get();
            } catch (Exception e) {
                return "someone";
            }
        }
    }

    public static boolean validateFlagTarget(String flag, String target) {
        switch(flag) {
            case GPFlags.BLOCK_BREAK :
            case GPFlags.BLOCK_PLACE :
            case GPFlags.ENTITY_COLLIDE_BLOCK :
            case GPFlags.PORTAL_USE :
                if (validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }

                return false;
            case GPFlags.ENTER_CLAIM :
            case GPFlags.EXIT_CLAIM :
            case GPFlags.ENTITY_RIDING :
                return validateEntityTarget(target);
            case GPFlags.ENTITY_DAMAGE :
                if (validateEntityTarget(target) ||
                    validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }

                return false;
            case GPFlags.INTERACT_INVENTORY :
            case GPFlags.LIQUID_FLOW :
                return validateBlockTarget(target);
            case GPFlags.INTERACT_BLOCK_PRIMARY :
            case GPFlags.INTERACT_BLOCK_SECONDARY :
                return validateBlockTarget(target);
            case GPFlags.INTERACT_ENTITY_PRIMARY :
            case GPFlags.INTERACT_ENTITY_SECONDARY :
                return validateEntityTarget(target);
            case GPFlags.ENTITY_SPAWN :
                if (validateEntityTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }

                return false;
            case GPFlags.ITEM_DROP :
            case GPFlags.ITEM_PICKUP :
            case GPFlags.ITEM_USE :
                return validateItemTarget(target);
            default :
                return true;
        }
    }

    private static boolean validateEntityTarget(String target) {
        Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
        if (entityType.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateItemTarget(String target) {
        Optional<ItemType> itemType = Sponge.getRegistry().getType(ItemType.class, target);
        if (itemType.isPresent()) {
            return true;
        }
        // target could be an item block, so validate blockstate
        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateBlockTarget(String target) {
        Optional<BlockType> blockType = Sponge.getRegistry().getType(BlockType.class, target);
        if (blockType.isPresent()) {
            return true;
        }

        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }
        return false;
    }

    public static Context validateCustomContext(CommandSource src, Claim claim, String context) {
        Context customContext = GriefPrevention.CUSTOM_CONTEXTS.get(context);
        if (customContext != null) {
            return customContext;
        } else if (context.equalsIgnoreCase("default") || context.equalsIgnoreCase("defaults")) {
            if (claim.isBasicClaim()) {
                return GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT;
            } else if (claim.isAdminClaim()) {
                return GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT;
            } else {
                src.sendMessage(Text.of(TextMode.Err, "Claim type " + claim.type.name() + " does not support flag defaults."));
                return null;
            }
        } else if (context.equalsIgnoreCase("override") || context.equalsIgnoreCase("overrides")) {
            if (claim.isBasicClaim()) {
                return GriefPrevention.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT;
            } else if (claim.isAdminClaim()) {
                return GriefPrevention.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT;
            } else {
                src.sendMessage(Text.of(TextMode.Err, "Claim type " + claim.type.name() + " does not support flag overrides."));
                return null;
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Context '" + context + "' was not found."));
            return null;
        }
    }

    public static CommandResult addPermission(CommandSource src, Subject subject, Claim claim, String flag, String target, Tristate value, Optional<String> context, int type) {
        if (src instanceof Player) {
            String denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, denyReason));
                return CommandResult.success();
            }
        }

        if (GPFlags.DEFAULT_FLAGS.get(flag) == null) {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
            return CommandResult.success();
        }

        String flagPermission = GPPermissions.FLAG_BASE + "." + flag;
        String targetFlag = flag;
        if (!target.equalsIgnoreCase("any")) {
            if (!target.contains(":")) {
                // assume vanilla
                target = "minecraft:" + target;
            }
            if (!CommandHelper.validateFlagTarget(flag, target)) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Invalid target id '" + target + "' entered for flag " + flag + "."));
                return CommandResult.success();
            }

            target = target.replace(":", ".").replace("[", ".[");
            flagPermission = GPPermissions.FLAG_BASE + "." + flag + "." + target;
            targetFlag = flag + "." + target;
        } else {
            target = "";
        }

        // check permission
        if (src.hasPermission(GPPermissions.MANAGE_FLAGS + "." + flag) || (!target.equals("") && src.hasPermission(GPPermissions.MANAGE_FLAGS + "." + targetFlag))) {
            Set<Context> contexts = new HashSet<>();
            Context customContext = null;
            if (context != null && context.isPresent()) {
                String targetContext = context.get();
                customContext = CommandHelper.validateCustomContext(src, claim, targetContext);
                if (customContext == null) {
                    GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "Context '" + targetContext + "' is invalid."));
                    return CommandResult.success();
                } else {
                    // validate perms
                    if (customContext == GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT || 
                            customContext == GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT || 
                            customContext == GriefPrevention.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT) {
                        if (!src.hasPermission(GPPermissions.MANAGE_FLAG_DEFAULTS)) {
                            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No permission to manage flag defaults."));
                            return CommandResult.success();
                        }
                    } else if (customContext == GriefPrevention.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT || 
                            customContext == GriefPrevention.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT) {
                        if (!src.hasPermission(GPPermissions.MANAGE_FLAG_OVERRIDES)) {
                            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No permission to manage flag overrides."));
                            return CommandResult.success();
                        }
                    }
                }
                contexts.add(customContext);
            }

            if (type == 0) {
                if (customContext == null || (customContext != GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT && customContext != GriefPrevention.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT
                        && customContext != GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT && customContext != GriefPrevention.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT)) {
                    contexts.add(claim.getContext());
                } else {
                    contexts.add(claim.world.getContext());
                }
                
                GriefPrevention.GLOBAL_SUBJECT.getSubjectData().setPermission(contexts, flagPermission, value);
                src.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", TextColors.AQUA, targetFlag, TextColors.GREEN, " to ", TextColors.LIGHT_PURPLE, value, TextColors.GREEN, " for ", TextColors.GOLD, "ALL."));
            } else if (type == 1) {
                contexts.add(claim.getContext());
                subject.getSubjectData().setPermission(contexts, flagPermission, value);
                src.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", TextColors.AQUA, targetFlag, TextColors.GREEN, " to ", TextColors.LIGHT_PURPLE, value, TextColors.GREEN, " for ", TextColors.GOLD, subject.getCommandSource().get().getName(), "."));
            } else if (type == 2) {
                contexts.add(claim.getContext());
                subject.getSubjectData().setPermission(contexts, flagPermission, value);
                src.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", TextColors.AQUA, targetFlag, TextColors.GREEN, " to ", TextColors.LIGHT_PURPLE, value, TextColors.GREEN, " for group ", TextColors.GOLD, subject.getIdentifier(), "."));
            }

            return CommandResult.success();
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No permission to use this flag."));
            return CommandResult.success();
        }
    }

    public static void handleTrustCommand(Player player, ClaimPermission claimPermission, String target) {

        User user = null;
        if (!target.equalsIgnoreCase("all") && !target.equalsIgnoreCase("public")) {
            user = GriefPrevention.instance.resolvePlayerByName(target).orElse(null);
        } else {
            user = GriefPrevention.PUBLIC_USER;
        }

        if (user == null) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return;
        }

        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, true);
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found at location. If you want to trust all claims, use /trustall instead."));
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(player) != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return;
            }
            
            //see if the player has the level of permission he's trying to grant
            String errorMessage = null;
            //permission level null indicates granting permission trust
            if(claimPermission == ClaimPermission.PERMISSION) {
                errorMessage = claim.allowEdit(player);
                if(errorMessage != null) {
                    //error message for trying to grant a permission the player doesn't have
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
                    return;
                }
            }

            targetClaims.add(claim);
        }

        String location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationCurrentClaim);
        for (Claim currentClaim : targetClaims) {
            ArrayList<UUID> memberList = null;
            if (claimPermission == ClaimPermission.ACCESS) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getAccessors();
            } else if (claimPermission == ClaimPermission.INVENTORY) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getContainers();
            } else if (claimPermission == ClaimPermission.BUILD) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getBuilders();
            } else if (claimPermission == ClaimPermission.PERMISSION) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getManagers();
            }

            if (memberList.contains(user.getUniqueId())) {
                String message = "Player " + target + " already has " + claimPermission + " permission.";
                if (user == GriefPrevention.PUBLIC_USER) {
                    message = "Public already has " + claimPermission + " permission.";
                }
                GriefPrevention.sendMessage(player, Text.of(TextMode.Info, message));
                return;
            } else {
                memberList.add(user.getUniqueId());
            }

            currentClaim.getClaimData().setRequiresSave(true);
        }

        //notify player
        String recipientName = user.getName();
        if(user.getName().equalsIgnoreCase("public") || user.getName().equalsIgnoreCase("all")) {
            recipientName = GriefPrevention.instance.dataStore.getMessage(Messages.CollectivePublic);
        }
        String permissionDescription;
        if(claimPermission == ClaimPermission.PERMISSION) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.PermissionsPermission);
        } else if(claimPermission == ClaimPermission.BUILD) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.BuildPermission);
        } else if(claimPermission == ClaimPermission.ACCESS) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.AccessPermission);
        } else {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.ContainersPermission);
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }
}
