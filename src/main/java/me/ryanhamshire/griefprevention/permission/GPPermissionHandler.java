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
package me.ryanhamshire.griefprevention.permission;

import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimContexts;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.action.CollideEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.LocatableBlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPPermissionHandler {

    private static Event currentEvent;
    private static Location<World> currentLocation;

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user) {
        return getClaimPermission(event, location, claim, flagPermission, source, target, user, null, false);
    }

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user, boolean checkOverride) {
        return getClaimPermission(event, location, claim, flagPermission, source, target, user, null, checkOverride);
    }

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user, TrustType type, boolean checkOverride) {
        if (claim == null) {
            return processResult(claim, flagPermission, Tristate.TRUE);
        }

        currentEvent = event;
        currentLocation = location;
        GPPlayerData playerData = null;
        if (user != null && user instanceof Player) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(claim.world, user.getUniqueId());
            if (playerData.canIgnoreClaim(claim)) {
                return processResult(claim, flagPermission, Tristate.TRUE);
            }
        }

        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        String targetModPermission = null;
        String targetMetaPermission = null;
        if (!targetId.isEmpty()) {
            if (!sourceId.isEmpty()) {
                String[] parts = targetId.split(":");
                String targetMod = parts[0];
                // move target meta to end of permission
                Pattern p = Pattern.compile("\\.[\\d+]*$");
                Matcher m = p.matcher(targetId);
                String targetMeta = "";
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = targetId.replace(targetMeta, "");
                }
                if (!targetMeta.isEmpty()) {
                    targetMetaPermission = flagPermission + "." + targetId.replace(":", ".") + targetMeta;
                }
                targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId + targetMeta;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetPermission += "." + targetId;
            }
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        if (checkOverride) {
            Tristate override = Tristate.UNDEFINED;
            if (user != null) {
                // check for bans first
                override = getFlagOverride(GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(claim.getWorld().getProperties()).getWildernessClaim(), user, targetPermission, targetModPermission, targetMetaPermission);
                if (override != Tristate.UNDEFINED) {
                    return override;
                }
            }
            // First check for claim flag overrides
            override = getFlagOverride(claim, user, targetPermission, targetModPermission, targetMetaPermission);
            if (override != Tristate.UNDEFINED) {
                return override;
            }
        }

        if (user != null) {
            if (type != null) {
                if (claim.isUserTrusted(user, type)) {
                    return Tristate.TRUE;
                }
            }
            return getUserPermission(user, claim, targetPermission, targetModPermission, targetMetaPermission, playerData);
        }

        return getClaimFlagPermission(claim, targetPermission, targetModPermission, targetMetaPermission);
    }

    private static Tristate getUserPermission(User user, GPClaim claim, String permission, String targetModPermission, String targetMetaPermission, GPPlayerData playerData) {
        final List<Claim> inheritParents = claim.getInheritedParents();
        if (playerData != null) {
            playerData.ignoreActiveContexts = true;
        }
        final Set<Context> contexts = new HashSet<>(user.getActiveContexts());
        if (playerData != null) {
            playerData.ignoreActiveContexts = false;
        }

        for (Claim parentClaim : inheritParents) {
            GPClaim parent = (GPClaim) parentClaim;
            // check parent context
            contexts.add(parent.getContext());  

            Tristate value = user.getPermissionValue(contexts, permission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, permission, value, user);
            }
            if (targetModPermission != null) {
                value = user.getPermissionValue(contexts, targetModPermission);
                if (value != Tristate.UNDEFINED) {
                    return processResult(claim, targetModPermission, value, user);
                }
            }

            contexts.remove(parent.getContext());
        }

        contexts.add(claim.getContext());
        Tristate value = user.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, user);
        }
        if (targetMetaPermission != null) {
            value = user.getPermissionValue(contexts, targetMetaPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetMetaPermission, value, user);
            }
        }
        if (targetModPermission != null) {
            value = user.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetModPermission, value, user);
            }
        }

        return getClaimFlagPermission(claim, permission, targetModPermission, targetMetaPermission);
    }

    private static Tristate getClaimFlagPermission(GPClaim claim, String permission, String targetModPermission, String targetMetaPermission) {
        Set<Context> contexts = new HashSet<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getActiveContexts());
        contexts.add(claim.getContext());

        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value);
        }
        if (targetMetaPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetMetaPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetMetaPermission, value);
            }
        }
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetModPermission, value);
            }
        }

        return getFlagDefaultPermission(claim, permission);
    }

    // Only uses world and claim type contexts
    private static Tristate getFlagDefaultPermission(GPClaim claim, String permission) {
        // Fallback to defaults
        Set<Context> contexts = new HashSet<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getActiveContexts());
        if (claim.parent != null && claim.getData().doesInheritParent()) {
            if (claim.parent.parent != null && claim.parent.getData().doesInheritParent()) {
                claim = claim.parent.parent;
            } else {
                claim = claim.parent;
            }
        }

        if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
        } else { // wilderness
            contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
        }

        contexts.add(claim.world.getContext());
        // check persisted/transient default data
        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value);
        }

        return processResult(claim, permission, Tristate.UNDEFINED);
    }

    private static Tristate getFlagOverride(GPClaim claim, User user, String flagPermission, String targetModPermission, String targetMetaPermission) {
        if (!claim.getInternalClaimData().allowFlagOverrides()) {
            return processResult(claim, flagPermission, Tristate.UNDEFINED);
        }

        Player player = null;
        Set<Context> contexts = new LinkedHashSet<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getActiveContexts());
        if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        } else if (claim.isBasicClaim()) {
            contexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        } else if (claim.isWilderness()) {
            contexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
            player = user instanceof Player ? (Player) user : null;
        }

        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, flagPermission);
        if (value != Tristate.UNDEFINED) {
            if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(flagPermission);
                if (reason != null && !reason.isEmpty()) {
                    player.sendMessage(reason);
                }
            }
            return processResult(claim, flagPermission, value, user);
        }
        if (targetMetaPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetMetaPermission);
            if (value != Tristate.UNDEFINED) {
                if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                    Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(targetMetaPermission);
                    if (reason != null && !reason.isEmpty()) {
                        player.sendMessage(reason);
                    }
                }
                return processResult(claim, targetMetaPermission, value, user);
            }
        }
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetModPermission, value, user);
            }
        }

        return processResult(claim, flagPermission, Tristate.UNDEFINED);
    }

    public static Tristate getFlagOverride(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target) {
        if (!claim.getInternalClaimData().allowFlagOverrides()) {
            return processResult(claim, flagPermission, Tristate.UNDEFINED);
        }

        currentEvent = event;
        currentLocation = location;

        final Player player = source instanceof Player ? (Player) source : null;
        if (player != null) {
            final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(claim.getWorld(), player.getUniqueId());
            if (playerData.canIgnoreClaim(claim)) {
                return processResult(claim, flagPermission, Tristate.TRUE);
            }
        }

        String targetModPermission = null;
        String targetMetaPermission = null;
        if (target != null && source == null) {
            String targetId = getPermissionIdentifier(target);
            flagPermission += "." + targetId;
        } else if (target != null && source != null) {
            String sourceId = getPermissionIdentifier(source, true);
            String targetId = getPermissionIdentifier(target);
            if (!targetId.isEmpty()) {
                String[] parts = targetId.split(":");
                String targetMod = parts[0];
                if (!sourceId.isEmpty()) {
                    // move target meta to end of permission
                    Pattern p = Pattern.compile("\\.[\\d+]*$");
                    Matcher m = p.matcher(targetId);
                    String targetMeta = "";
                    if (m.find()) {
                        targetMeta = m.group(0);
                        targetId = targetId.replace(targetMeta, "");
                    }
                    if (!targetMeta.isEmpty()) {
                        targetMetaPermission = flagPermission + "." + targetId.replace(":", ".") + targetMeta;
                    }
                    targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId + targetMeta;
                    targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                    flagPermission += "." + targetId + ".source." + sourceId + targetMeta;
                } else {
                    flagPermission += "." + targetId;
                }
            }
        }

        flagPermission = StringUtils.replace(flagPermission, ":", ".");
        Set<Context> contexts = new LinkedHashSet<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getActiveContexts());
        if (claim.isWilderness()) {
            contexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
        } else if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        } else {
            contexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
            contexts.add(claim.world.getContext());
        }

        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, flagPermission);
        if (value != Tristate.UNDEFINED) {
            if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(flagPermission);
                if (reason != null && !reason.isEmpty()) {
                    player.sendMessage(reason);
                }
            }
            return processResult(claim, flagPermission, value);
        }
        if (targetMetaPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetMetaPermission);
            if (value != Tristate.UNDEFINED) {
                if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                    Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(targetMetaPermission);
                    if (reason != null && !reason.isEmpty()) {
                        player.sendMessage(reason);
                    }
                }
                return processResult(claim, targetMetaPermission, value);
            }
        }
        // check target modid
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetModPermission, value);
            }
        }

        return processResult(claim, flagPermission, Tristate.UNDEFINED);
    }

    // used by Flag API
    public static Tristate getClaimPermission(GPClaim claim, ClaimFlag flag, Subject subject, String source, String target, Context context) {
        final String flagBasePermission = GPPermissions.FLAG_BASE + "." + flag.toString();
        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagBasePermission;
        String targetId = getPermissionIdentifier(target);
        String targetModPermission = null;
        if (!targetId.isEmpty()) {
            if (!sourceId.isEmpty()) {
                String[] parts = targetId.split(":");
                String targetMod = parts[0];
                // move target meta to end of permission
                Pattern p = Pattern.compile("\\.[\\d+]*$");
                Matcher m = p.matcher(targetId);
                String targetMeta = "";
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = targetId.replace(targetMeta, "");
                }
                targetModPermission = flagBasePermission + "." + targetMod + ".source." + sourceId + targetMeta;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetPermission += "." + targetId;
            }
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        Set<Context> contexts = new HashSet<>();
        contexts.add(context);
        return subject.getPermissionValue(contexts, targetPermission);
    }

    public static Tristate processResult(GPClaim claim, String permission, Tristate permissionValue) {
        return processResult(claim, permission, permissionValue, null);
    }

    public static Tristate processResult(GPClaim claim, String permission, Tristate permissionValue, User user) {
        if (permissionValue == Tristate.FALSE) {
            if (currentEvent instanceof CollideEvent || currentEvent instanceof NotifyNeighborBlockEvent) {
                if (claim.getWorld().getProperties().getTotalTime() % 100 == 0L) {
                    GriefPreventionPlugin.addEventLogEntry(currentEvent, claim, currentLocation, user, permission);
                }
            } else {
                GriefPreventionPlugin.addEventLogEntry(currentEvent, claim, currentLocation, user, permission);
            }
        }
        return permissionValue;
    }

    public static String getPermissionIdentifier(Object obj) {
        return getPermissionIdentifier(obj, false);
    }

    @SuppressWarnings("deprecation")
    public static String getPermissionIdentifier(Object obj, boolean isSource) {
        if (obj != null) {
            if (obj instanceof Entity) {
                Entity targetEntity = (Entity) obj;
                net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) targetEntity;
                String targetId = "";
                if (mcEntity instanceof EntityItem) {
                    EntityItem mcItem = (EntityItem) mcEntity;
                    net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) mcItem.getEntityItem();
                    if (itemStack != null && itemStack.getItem() != null) {
                        ItemType itemType = (ItemType) itemStack.getItem();
                        targetId = itemType.getId() + "." + itemStack.getItemDamage();
                    }
                } else {
                    if (targetEntity.getType() != null) {
                        targetId = targetEntity.getType().getId();
                    }
                }
                // Workaround for pixelmon using same class for most entities.
                // In this circumstance, we will use the entity name instead
                if (targetId.equals("pixelmon:pixelmon")) {
                    targetId = "pixelmon:" + mcEntity.getName().toLowerCase();
                }
                if (!isSource && targetEntity instanceof Living) {
                    for (EnumCreatureType type : EnumCreatureType.values()) {
                        if (SpongeImplHooks.isCreatureOfType(mcEntity, type)) {
                            String[] parts = targetId.split(":");
                            if (parts.length > 1) {
                                targetId =  parts[0] + ":" + GPFlags.SPAWN_TYPES.inverse().get(type) + ":" + parts[1];
                                break;
                            }
                        }
                    }
                }

                if (targetEntity instanceof Item) {
                    targetId = ((Item) targetEntity).getItemType().getId();
                }

                return targetId.toLowerCase();
            } else if (obj instanceof EntityType) {
                return ((EntityType) obj).getId();
            } else if (obj instanceof SpawnCause) {
                SpawnCause spawnCause = (SpawnCause) obj;
                if (spawnCause instanceof EntitySpawnCause) {
                    EntitySpawnCause entitySpawnCause = (EntitySpawnCause) spawnCause;
                    return getPermissionIdentifier(entitySpawnCause.getEntity(), true);
                } else if (spawnCause instanceof BlockSpawnCause) {
                    BlockSpawnCause blockSpawnCause = (BlockSpawnCause) spawnCause;
                    return getPermissionIdentifier(blockSpawnCause.getBlockSnapshot(), true);
                } else if (spawnCause instanceof LocatableBlockSpawnCause) {
                    LocatableBlockSpawnCause locatableSpawnCause = (LocatableBlockSpawnCause) spawnCause;
                    return getPermissionIdentifier(locatableSpawnCause.getLocatableBlock(), true);
                }
            } else if (obj instanceof BlockType) {
                String targetId = ((BlockType) obj).getId();
                return targetId;
            } else if (obj instanceof BlockSnapshot) {
                BlockSnapshot blockSnapshot = (BlockSnapshot) obj;
                BlockState blockstate = blockSnapshot.getState();
                String targetId = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                return targetId.toLowerCase();
            } else if (obj instanceof BlockState) {
                BlockState blockstate = (BlockState) obj;
                String targetId = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                return targetId.toLowerCase();
            } else if (obj instanceof LocatableBlock) {
                LocatableBlock locatableBlock = (LocatableBlock) obj;
                BlockState blockstate = locatableBlock.getBlockState();
                String targetId = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                return targetId.toLowerCase();
            } else if (obj instanceof ItemStack) {
                ItemStack itemstack = (ItemStack) obj;
                String targetId = "";
                if (itemstack.getItem() instanceof ItemBlock) {
                    ItemBlock itemBlock = (ItemBlock) itemstack.getItem();
                    net.minecraft.item.ItemStack nmsStack = (net.minecraft.item.ItemStack)(Object) itemstack;
                    BlockState blockState = ((BlockState) itemBlock.getBlock().getStateFromMeta(nmsStack.getItemDamage()));
                    targetId = blockState.getType().getId() + "." + nmsStack.getItemDamage();//.replace("[", ".[");
                } else {
                    targetId = itemstack.getItem().getId() + "." + ((net.minecraft.item.ItemStack)(Object) itemstack).getItemDamage();
                }

                return targetId.toLowerCase();
            } else if (obj instanceof ItemType) {
                String targetId = ((ItemType) obj).getId().toLowerCase();
                return targetId;
            } else if (obj instanceof DamageSource) {
                final DamageSource damageSource = (DamageSource) obj;
                String damageTypeId = damageSource.getType().getId();
                if (!damageTypeId.contains(":")) {
                    damageTypeId = "minecraft:" + damageTypeId;
                }

                return damageTypeId;
            } else if (obj instanceof CatalogType) {
                return ((CatalogType) obj).getId();
            } else if (obj instanceof String) {
                return obj.toString().toLowerCase();
            } else if (obj instanceof PluginContainer) {
                return ((PluginContainer) obj).getId();
            }
        }

        return "";
    }

    public static ClaimFlag getFlagFromPermission(String flagPermission) {
        for (ClaimFlag flag : ClaimFlag.values()) {
            if (flagPermission.contains(flag.toString())) {
                return flag;
            }
        }
        return null;
    }

    public static String getSourcePermission(String flagPermission) {
        final int index = flagPermission.indexOf(".source.");
        if (index != -1) {
            return flagPermission.substring(index + 8);
        }

        return null;
    }

    public static String getTargetPermission(String flagPermission) {
        flagPermission = flagPermission.replace("griefprevention.flag.", "");
        boolean found = false;
        for (ClaimFlag flag : ClaimFlag.values()) {
            if (flagPermission.contains(flag.toString() + ".")) {
                found = true;
            }
            flagPermission = flagPermission.replace(flag.toString() + ".", "");
        }
        if (!found) {
            return null;
        }
        final int sourceIndex = flagPermission.indexOf(".source.");
        if (sourceIndex != -1) {
            flagPermission = flagPermission.replace(flagPermission.substring(sourceIndex, flagPermission.length()), "");
        }

        return flagPermission;
    }

    // Used for debugging
    public static String getPermission(Object source, Object target, String flagPermission) {
        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        if (!targetId.isEmpty()) {
            if (!sourceId.isEmpty()) {
                // move target meta to end of permission
                Pattern p = Pattern.compile("\\.[\\d+]*$");
                Matcher m = p.matcher(targetId);
                String targetMeta = "";
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = targetId.replace(targetMeta, "");
        }
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetPermission += "." + targetId;
            }
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        return targetPermission;
    }

    public static String getIdentifierWithoutMeta(String targetId) {
        Pattern p = Pattern.compile("\\.[\\d+]*$");
        Matcher m = p.matcher(targetId);
        String targetMeta = "";
        if (m.find()) {
            targetMeta = m.group(0);
            targetId = targetId.replace(targetMeta, "");
        }
        return targetId;
    }
}
