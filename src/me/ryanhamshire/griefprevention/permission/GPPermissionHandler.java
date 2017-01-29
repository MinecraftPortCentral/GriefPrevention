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
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeImplHooks;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPPermissionHandler {

    public static Tristate getClaimPermission(GPClaim claim, String flagPermission, Object source, Object target, User user) {
        return getClaimPermission(claim, flagPermission, source, target, user, false);
    }

    public static Tristate getClaimPermission(GPClaim claim, String flagPermission, Object source, Object target, User user, boolean checkOverride) {
        if (claim == null) {
            return Tristate.UNDEFINED;
        }
        if (user != null) {
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(claim.world, user.getUniqueId());
            if (playerData.canIgnoreClaim(claim)) {
                return Tristate.TRUE;
            }
        }

        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
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
                targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId + targetMeta;
                targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetPermission += "." + targetId;
            }
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");

        if (checkOverride) {
            // First check for claim flag overrides
            Tristate override = getFlagOverride(claim, targetPermission, targetModPermission);
            if (override != Tristate.UNDEFINED) {
                return override;
            }
        }

        if (user != null) {
            if (claim.hasFullAccess(user)) {
                return Tristate.TRUE;
            }

            return getUserPermission(user, claim, targetPermission, targetModPermission);
        }

        return getClaimFlagPermission(claim, targetPermission, targetModPermission);
    }

    public static Tristate getUserPermission(User user, GPClaim claim, String permission, String targetModPermission) {
        Set<Context> contexts = new HashSet<>();
        if (claim.parent != null && claim.getData().doesInheritParent()) {
            // check subdivision's parent
            contexts.add(claim.parent.getContext());
        } else {
            contexts.add(claim.getContext());
        }

        Tristate value = user.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }
        if (targetModPermission != null) {
            value = user.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return getClaimFlagPermission(claim, permission, targetModPermission, contexts);
    }

    // helper method for boolean flags that have no targets
    public static Tristate getClaimBooleanFlagPermission(GPClaim claim, String permission) {
        // First check for claim flag overrides
        Tristate value = getFlagOverride(claim, permission, null);
        if (value != Tristate.UNDEFINED) {
            return value;
        }

        return getClaimFlagPermission(claim, permission, null);
    }

    public static Tristate getClaimFlagPermission(GPClaim claim, String permission, String targetModPermission) {
        return getClaimFlagPermission(claim, permission, targetModPermission, null);
    }

    public static Tristate getClaimFlagPermission(GPClaim claim, String permission, String targetModPermission, Set<Context> contexts) {
        if (contexts == null) {
            contexts = new HashSet<>();
            if (claim.parent != null && claim.getData().doesInheritParent()) {
                // check subdivision's parent
                contexts.add(claim.parent.getContext());
            } else {
                contexts.add(claim.getContext());
            }
        }

        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return getFlagDefaultPermission(claim, permission);
    }

    // Only uses world and claim type contexts
    private static Tristate getFlagDefaultPermission(GPClaim claim, String permission) {
        // Fallback to defaults
        Set<Context> contexts = new HashSet<>();
        if (claim.isAdminClaim()) {
            contexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            contexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
        } else { // wilderness
            contexts.add(GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
        }

        contexts.add(claim.world.getContext());
        // check persisted/transient default data
        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }

        return Tristate.UNDEFINED;
    }

    public static Tristate getFlagOverride(GPClaim claim, String flagPermission, String targetModPermission) {
        Set<Context> contexts = new LinkedHashSet<>();
        if (claim.isAdminClaim()) {
            contexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT);
        } else {
            contexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT);
        }
        contexts.add(claim.world.getContext());
        Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, flagPermission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }
        if (targetModPermission != null) {
            value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return Tristate.UNDEFINED;
    }

    public static Tristate getFlagOverride(GPClaim claim, String flagPermission, Object source, Object target) {
        // First check for claim flag overrides
        if (!claim.isWildernessClaim()) {
            if (!claim.getInternalClaimData().allowFlagOverrides()) {
                return Tristate.UNDEFINED;
            }

            String targetModPermission = null;
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
                        targetModPermission = flagPermission + "." + targetMod + ".source." + sourceId + targetMeta;
                        targetModPermission = StringUtils.replace(targetModPermission, ":", ".");
                        flagPermission += "." + targetId + ".source." + sourceId + targetMeta;
                    } else {
                        flagPermission += "." + targetId;
                    }
                }
            }

            flagPermission = StringUtils.replace(flagPermission, ":", ".");
            Set<Context> contexts = new LinkedHashSet<>();
            if (claim.isAdminClaim()) {
                contexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT);
            } else {
                contexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT);
            }
            contexts.add(claim.world.getContext());
            Tristate value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, flagPermission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
            // check target modid
            if (targetModPermission != null) {
                value = GriefPreventionPlugin.GLOBAL_SUBJECT.getPermissionValue(contexts, targetModPermission);
                if (value != Tristate.UNDEFINED) {
                    return value;
                }
            }
        }

        return Tristate.UNDEFINED;
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
                            targetId =  parts[0] + ":" + GPFlags.SPAWN_TYPES.inverse().get(type) + ":" + parts[1];
                            break;
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
            } else if (obj instanceof String) {
                return obj.toString().toLowerCase();
            } else if (obj instanceof PluginContainer) {
                return ((PluginContainer) obj).getId();
            }
        }

        return "";
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
}
