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
import me.ryanhamshire.griefprevention.util.EntityUtils;
import me.ryanhamshire.griefprevention.util.PermissionUtils;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
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
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPPermissionHandler {

    private static Event currentEvent;
    private static Location<World> eventLocation;
    private static Subject eventSubject;
    private static String eventSourceId = "none";
    private static String eventTargetId = "none";
    private static final Pattern PATTERN_META = Pattern.compile("\\.[\\d+]*$");

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user) {
        return getClaimPermission(event, location, claim, flagPermission, source, target, user, null, false);
    }

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user, boolean checkOverride) {
        return getClaimPermission(event, location, claim, flagPermission, source, target, user, null, checkOverride);
    }

    public static Tristate getClaimPermission(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user, TrustType type, boolean checkOverride) {
        if (claim == null) {
            return Tristate.TRUE;
        }

        GPPlayerData playerData = null;
        eventSubject = null;
        if (user != null) {
            eventSubject = user;
            if (user instanceof Player) {
                playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(claim.world, user.getUniqueId());
            }
        }
        currentEvent = event;
        eventLocation = location;

        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        String targetModPermission = null;
        String targetMetaPermission = null;
        if (!targetId.isEmpty()) {
            String[] parts = targetId.split(":");
            String targetMod = parts[0];
            // move target meta to end of permission
            Matcher m = PATTERN_META.matcher(targetId);
            String targetMeta = "";
            if (!flagPermission.contains("command-execute")) {
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = StringUtils.replace(targetId, targetMeta, "");
                }
                if (!targetMeta.isEmpty()) {
                    targetMetaPermission = flagPermission + "." + StringUtils.replace(targetId, ":", ".") + targetMeta;
                }
            }
            if (!sourceId.isEmpty()) {
                targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId + targetMeta;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetModPermission = flagPermission + "." + targetMod + targetMeta;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                targetPermission += "." + targetId + targetMeta;
            }
        } else if (!sourceId.isEmpty()) {
            String targetMod = "";
            if (sourceId.contains(":")) {
                String[] parts = sourceId.split(":");
                targetMod = parts[0];
                targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
            }
            targetPermission += ".source." + sourceId;
        }

        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        if (user != null && playerData != null && !playerData.debugClaimPermissions && playerData.canIgnoreClaim(claim)) {
            return processResult(claim, targetPermission, "ignore", Tristate.TRUE, user);
        }
        if (checkOverride) {
            Tristate override = Tristate.UNDEFINED;
            if (user != null) {
                // check global bans in wilderness
                override = getFlagOverride((GPClaim) claim.getWilderness(), user, user, playerData, targetPermission, targetModPermission, targetMetaPermission);
                if (override != Tristate.UNDEFINED) {
                    return override;
                }
            }
            // First check for claim flag overrides
            override = getFlagOverride(claim, user == null ? GriefPreventionPlugin.GLOBAL_SUBJECT : user, user, playerData, targetPermission, targetModPermission, targetMetaPermission);
            if (override != Tristate.UNDEFINED) {
                return override;
            }
        }

        if (playerData != null) {
            if (playerData.debugClaimPermissions) {
                if (user != null && type != null && claim.isUserTrusted(user, type)) {
                    return processResult(claim, targetPermission, type.toString().toLowerCase(), Tristate.TRUE, user);
                }
                return getClaimFlagPermission(claim, targetPermission, targetModPermission, targetMetaPermission);
            }
        }
        if (user != null) {
            if (type != null) {
                if (claim.isUserTrusted(user, type)) {
                    return processResult(claim, targetPermission, type.toString().toLowerCase(), Tristate.TRUE, user);
                }
            }
            return getUserPermission(user, claim, targetPermission, targetModPermission, targetMetaPermission, playerData);
        }

        return getClaimFlagPermission(claim, targetPermission, targetModPermission, targetMetaPermission);
    }

    private static Tristate getUserPermission(User user, GPClaim claim, String permission, String targetModPermission, String targetMetaPermission, GPPlayerData playerData) {
        final List<Claim> inheritParents = claim.getInheritedParents();
        final Set<Context> contexts = PermissionUtils.getActiveContexts(user, playerData, permission);

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
            return processResult(claim, permission, value, GriefPreventionPlugin.GLOBAL_SUBJECT);
        }
        if (targetMetaPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetMetaPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetMetaPermission, value, GriefPreventionPlugin.GLOBAL_SUBJECT);
            }
        }
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, targetModPermission, value, GriefPreventionPlugin.GLOBAL_SUBJECT);
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
            return processResult(claim, permission, value, GriefPreventionPlugin.GLOBAL_SUBJECT);
        }

        return processResult(claim, permission, Tristate.UNDEFINED, GriefPreventionPlugin.GLOBAL_SUBJECT);
    }

    private static Tristate getFlagOverride(GPClaim claim, Subject subject, User user, GPPlayerData playerData, String flagPermission, String targetModPermission, String targetMetaPermission) {
        if (!claim.getInternalClaimData().allowFlagOverrides()) {
            return Tristate.UNDEFINED;
        }
        if (playerData != null && !playerData.debugClaimPermissions && playerData.canIgnoreClaim(claim)) {
            return Tristate.TRUE;
        }

        Player player = null;
        Set<Context> contexts = PermissionUtils.getActiveContexts(subject, playerData, null);
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

        Tristate value = subject.getPermissionValue(contexts, flagPermission);
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
            value = subject.getPermissionValue(contexts, targetMetaPermission);
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
            value = subject.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                    Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(targetModPermission);
                    if (reason != null && !reason.isEmpty()) {
                        player.sendMessage(reason);
                    }
                }
                return processResult(claim, targetModPermission, value, user);
            }
        }

        return Tristate.UNDEFINED;
    }

    public static Tristate getFlagOverride(Event event, Location<World> location, GPClaim claim, String flagPermission, Object source, Object target, User user, GPPlayerData playerData, boolean checkWildernessOverride) {
        if (!claim.getInternalClaimData().allowFlagOverrides()) {
            return Tristate.UNDEFINED;
        }
        if (playerData != null && !playerData.debugClaimPermissions && playerData.canIgnoreClaim(claim)) {
            return Tristate.TRUE;
        }

        if (checkWildernessOverride && !claim.isWilderness()) {
            final Tristate wildernessOverride = getFlagOverride(event, location, (GPClaim) claim.getWilderness(), flagPermission, source, target, user, playerData, false);
            if (wildernessOverride != Tristate.UNDEFINED) {
                return wildernessOverride;
            }
        }

        currentEvent = event;
        eventLocation = location;
        eventSubject = user;
        Player player = null;
        final Subject subject = user != null ? user : GriefPreventionPlugin.GLOBAL_SUBJECT;
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
                    Matcher m = PATTERN_META.matcher(targetId);
                    String targetMeta = "";
                    if (m.find()) {
                        targetMeta = m.group(0);
                        targetId = StringUtils.replace(targetId, targetMeta, "");
                    }
                    if (!targetMeta.isEmpty()) {
                        targetMetaPermission = flagPermission + "." + StringUtils.replace(targetId, ":", ".") + targetMeta;
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
        Set<Context> contexts = PermissionUtils.getActiveContexts(subject, playerData, null);
        if (claim.isWilderness()) {
            contexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
            player = user instanceof Player ? (Player) user : null;
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

        Tristate value = subject.getPermissionValue(contexts, flagPermission);
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
            value = subject.getPermissionValue(contexts, targetMetaPermission);
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
        // check target modid
        if (targetModPermission != null) {
            value = subject.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                    Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(targetModPermission);
                    if (reason != null && !reason.isEmpty()) {
                        player.sendMessage(reason);
                    }
                }
                return processResult(claim, targetModPermission, value, user);
            }
        }

        return Tristate.UNDEFINED;
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
                    targetId = StringUtils.replace(targetId, targetMeta, "");
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

    public static Tristate processResult(GPClaim claim, String permission, Tristate permissionValue, Subject permissionSubject) {
        return processResult(claim, permission, null, permissionValue, permissionSubject);
    }

    public static Tristate processResult(GPClaim claim, String permission, String trust, Tristate permissionValue, Subject permissionSubject) {
        if (GriefPreventionPlugin.debugActive) {
            // Use the event subject always if available
            // This prevents debug showing 'default' for users
            if (eventSubject != null) {
                permissionSubject = eventSubject;
            } else if (permissionSubject == null) {
                if (currentEvent.getCause().root() instanceof User) {
                    permissionSubject = (Subject) currentEvent.getCause().root();
                } else {
                    permissionSubject = GriefPreventionPlugin.GLOBAL_SUBJECT;
                }
            }
            if (currentEvent instanceof NotifyNeighborBlockEvent) {
                if (claim.getWorld().getProperties().getTotalTime() % 100 == 0L) {
                    GriefPreventionPlugin.addEventLogEntry(currentEvent, eventLocation, eventSourceId, eventTargetId, permissionSubject, permission, trust, permissionValue);
                }
            } else {
                GriefPreventionPlugin.addEventLogEntry(currentEvent, eventLocation, eventSourceId, eventTargetId, permissionSubject, permission, trust, permissionValue);
            }
        }

        return permissionValue;
    }

    // Used for situations where events are skipped for perf reasons
    public static void addEventLogEntry(Event event, Location<World> location, Object source, Object target, Subject permissionSubject, String permission, String trust, Tristate result) {
        if (GriefPreventionPlugin.debugActive) {
            String sourceId = getPermissionIdentifier(source, true);
            String targetPermission = permission;
            String targetId = getPermissionIdentifier(target);
            if (!targetId.isEmpty()) {
                // move target meta to end of permission
                Matcher m = PATTERN_META.matcher(targetId);
                String targetMeta = "";
                if (!permission.contains("command-execute")) {
                    if (m.find()) {
                        targetMeta = m.group(0);
                        targetId = StringUtils.replace(targetId, targetMeta, "");
                    }
                }
                targetPermission += "." + targetId + targetMeta;
            }
            if (permissionSubject == null) {
                permissionSubject = GriefPreventionPlugin.GLOBAL_SUBJECT;
            }
            GriefPreventionPlugin.addEventLogEntry(event, location, sourceId, targetId, permissionSubject, targetPermission, trust, result);
        }
    }

    public static String getPermissionIdentifier(Object obj) {
        return getPermissionIdentifier(obj, false);
    }

    @SuppressWarnings("deprecation")
    public static String getPermissionIdentifier(Object obj, boolean isSource) {
        if (obj != null) {
            if (obj instanceof Entity) {
                Entity targetEntity = (Entity) obj;
                net.minecraft.entity.Entity mcEntity = null;
                if (targetEntity instanceof net.minecraft.entity.Entity) {
                    mcEntity = (net.minecraft.entity.Entity) targetEntity;
                }
                String id = "";
                if (mcEntity != null && mcEntity instanceof EntityItem) {
                    EntityItem mcItem = (EntityItem) mcEntity;
                    net.minecraft.item.ItemStack itemStack = mcItem.getItem();
                    if (itemStack != null && itemStack.getItem() != null) {
                        ItemType itemType = (ItemType) itemStack.getItem();
                        id = itemType.getId() + "." + itemStack.getItemDamage();
                    }
                } else {
                    if (targetEntity.getType() != null) {
                        id = targetEntity.getType().getId();
                    }
                }

                if (mcEntity != null && id.contains("unknown") && SpongeImplHooks.isFakePlayer(mcEntity)) {
                    final String modId = SpongeImplHooks.getModIdFromClass(mcEntity.getClass());
                    id = modId + ":fakeplayer_" + EntityUtils.getFriendlyName(mcEntity).toLowerCase();
                } else if (id.equals("unknown:unknown") && obj instanceof EntityPlayer) {
                    id = "minecraft:player";
                }

                if (mcEntity != null && targetEntity instanceof Living) {
                    String[] parts = id.split(":");
                    if (parts.length > 1) {
                        final String modId = parts[0];
                        String name = parts[1];
                        if (modId.equalsIgnoreCase("pixelmon") && modId.equalsIgnoreCase(name)) {
                            name = EntityUtils.getFriendlyName(mcEntity).toLowerCase();
                            populateEventSourceTarget(modId + ":" + name, isSource);
                        }
                        if (!isSource) {
                            for (EnumCreatureType type : EnumCreatureType.values()) {
                                if (SpongeImplHooks.isCreatureOfType(mcEntity, type)) {
                                    id = modId + ":" + GPFlags.SPAWN_TYPES.inverse().get(type) + ":" + name;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (targetEntity instanceof Item) {
                    id = ((Item) targetEntity).getItemType().getId();
                }

                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof EntityType) {
                final String id = ((EntityType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockType) {
                final String id = ((BlockType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockSnapshot) {
                final BlockSnapshot blockSnapshot = (BlockSnapshot) obj;
                final BlockState blockstate = blockSnapshot.getState();
                String id = "";
                if (currentEvent != null && !(currentEvent instanceof ChangeBlockEvent.Pre)) {
                    id = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                } else {
                    id = blockstate.getType().getId();
                }
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockState) {
                final BlockState blockstate = (BlockState) obj;
                final String id = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof LocatableBlock) {
                final LocatableBlock locatableBlock = (LocatableBlock) obj;
                final BlockState blockstate = locatableBlock.getBlockState();
                final String id = blockstate.getType().getId() + "." + BlockUtils.getBlockStateMeta(blockstate);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof TileEntity) {
                TileEntity tileEntity = (TileEntity) obj;
                final String id = tileEntity.getType().getId().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof ItemStack) {
                final ItemStack itemstack = (ItemStack) obj;
                String id = "";
                if (itemstack.getType() instanceof ItemBlock) {
                    ItemBlock itemBlock = (ItemBlock) itemstack.getType();
                    net.minecraft.item.ItemStack nmsStack = (net.minecraft.item.ItemStack)(Object) itemstack;
                    BlockState blockState = ((BlockState) itemBlock.getBlock().getStateFromMeta(nmsStack.getItemDamage()));
                    id = blockState.getType().getId() + "." + nmsStack.getItemDamage();
                } else {
                    id = itemstack.getType().getId() + "." + ((net.minecraft.item.ItemStack)(Object) itemstack).getItemDamage();
                }

                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof ItemType) {
                final String id = ((ItemType) obj).getId().toLowerCase();
                populateEventSourceTarget(id, isSource);
                return id;
            } else if (obj instanceof EntityDamageSource) {
                final EntityDamageSource damageSource = (EntityDamageSource) obj;
                Entity sourceEntity = damageSource.getSource();

                if (eventSubject == null && sourceEntity instanceof User) {
                    eventSubject = (User) sourceEntity;
                }

                return getPermissionIdentifier(sourceEntity, isSource);
            } else if (obj instanceof DamageSource) {
                final DamageSource damageSource = (DamageSource) obj;
                String id = damageSource.getType().getId();
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }

                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof ItemStackSnapshot) {
                final String id = ((ItemStackSnapshot) obj).getType().getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof CatalogType) {
                final String id = ((CatalogType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof String) {
                final String id = obj.toString().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof PluginContainer) {
                final String id = ((PluginContainer) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof Inventory) {
                return ((Inventory) obj).getArchetype().getId();
            }
        }

        populateEventSourceTarget("none", isSource);
        return "";
    }

    public static ClaimFlag getFlagFromPermission(String flagPermission) {
        try {
            return ClaimFlag.getEnum(flagPermission);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getSourcePermission(String flagPermission) {
        final int index = flagPermission.indexOf(".source.");
        if (index != -1) {
            return flagPermission.substring(index + 8);
        }

        return null;
    }

    public static String getTargetPermission(String flagPermission) {
        flagPermission = StringUtils.replace(flagPermission, "griefprevention.flag.", "");
        boolean found = false;
        for (ClaimFlag flag : ClaimFlag.values()) {
            if (flagPermission.contains(flag.toString() + ".")) {
                found = true;
            }
            flagPermission = StringUtils.replace(flagPermission, flag.toString() + ".", "");
        }
        if (!found) {
            return null;
        }
        final int sourceIndex = flagPermission.indexOf(".source.");
        if (sourceIndex != -1) {
            flagPermission = StringUtils.replace(flagPermission, flagPermission.substring(sourceIndex, flagPermission.length()), "");
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
                Matcher m = PATTERN_META.matcher(targetId);
                String targetMeta = "";
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = StringUtils.replace(targetId, targetMeta, "");
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
        Matcher m = PATTERN_META.matcher(targetId);
        String targetMeta = "";
        if (m.find()) {
            targetMeta = m.group(0);
            targetId = StringUtils.replace(targetId, targetMeta, "");
        }
        return targetId;
    }

    private static String populateEventSourceTarget(String id, boolean isSource) {
        // Handle mod's that pass modid:modid:name
        String[] parts = id.split(":");
        if (parts != null && parts.length == 3) {
            if (parts[0].equals(parts[1])) {
                id = parts[1] + ":" + parts[2];
            }
        }
        id = id.toLowerCase();
        if (isSource) {
            eventSourceId = id;
        } else {
            eventTargetId = id;
        }
        return id;
    }
}
