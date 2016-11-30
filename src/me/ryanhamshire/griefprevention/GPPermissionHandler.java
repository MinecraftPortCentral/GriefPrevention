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
package me.ryanhamshire.griefprevention;

import me.ryanhamshire.griefprevention.claim.Claim;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.SpongeImplHooks;

import java.util.HashSet;
import java.util.Set;

public class GPPermissionHandler {

    private static final String UNKNOWN_CONTEXT = "unknown:unknown";

    public static Tristate getClaimPermission(Claim claim, String flagPermission, Object source, Object target, User user) {
        if (claim == null) {
            return Tristate.UNDEFINED;
        }
        if (user != null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(claim.world, user.getUniqueId());
            if (playerData.ignoreClaims) {
                return Tristate.TRUE;
            }
        }

        String sourceId = getPermissionIdentifier(source);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        if (!targetId.isEmpty()) {
            targetPermission += "." + targetId;
        }

        // first check source for deny
        Context sourceContext = GriefPrevention.CUSTOM_CONTEXTS.get(sourceId);
        targetPermission = targetPermission.replace(":", ".");
        // First check for claim flag overrides
        Tristate override = getFlagOverride(claim, targetPermission);
        if (override != Tristate.UNDEFINED) {
            return override;
        }

        if (user != null) {
            if (claim.hasFullAccess(user)) {
                return Tristate.TRUE;
            }

            return getUserPermission(user, claim, targetPermission, sourceContext);
        }

        if (sourceContext != null && getClaimFlagPermission(claim, targetPermission, sourceContext) == Tristate.FALSE) {
            return Tristate.FALSE;
        }

        return getClaimFlagPermission(claim, targetPermission, null);
    }

    public static Tristate getUserPermission(User user, Claim claim, String permission, Context sourceContext) {
        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());

        if (sourceContext != null) {
            contexts.add(sourceContext);
        }

        Tristate value = user.getPermissionValue(contexts, permission);
        if (value == Tristate.UNDEFINED && claim.parent != null && claim.inheritParent) {
            // check subdivision's parent
            contexts.remove(claim.getContext());
            contexts.add(claim.parent.getContext());
            value = user.getPermissionValue(contexts, permission);
        }
        // If source context is not null, check with mod id this time
        if (sourceContext != null) {
            String modId = sourceContext.getValue().split(":")[0];
            Context modContext = GriefPrevention.CUSTOM_CONTEXTS.get(modId);
            contexts = new HashSet<>();
            if (modContext != null) {
                contexts.add(modContext);
            }
            contexts.add(claim.getContext());
            Tristate value2 = user.getPermissionValue(contexts, permission);
            if (value2 == Tristate.UNDEFINED && claim.parent != null && claim.inheritParent) {
                // check subdivision's parent
                contexts.remove(claim.getContext());
                contexts.add(claim.parent.getContext());
                value2 = user.getPermissionValue(contexts, permission);
            }
            if (value2 != Tristate.UNDEFINED) {
                return value2;
            }
        }

        if (value == Tristate.UNDEFINED) {
            return getFlagDefaultPermission(claim, permission);
        }
        return value;
    }

    // helper method for boolean flags that have no targets
    public static Tristate getClaimBooleanFlagPermission(Claim claim, String permission) {
        // First check for claim flag overrides
        Tristate value = getFlagOverride(claim, permission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }

        return getClaimFlagPermission(claim, permission, null);
    }

    public static Tristate getClaimFlagPermission(Claim claim, String permission, Context sourceContext) {
        Tristate value = Tristate.UNDEFINED;
        // Strip base permission node to keep it simpler locally
        Set<Context> contexts = new HashSet<>();
        if (sourceContext != null) {
            contexts.add(sourceContext);
        }

        contexts.add(claim.getContext());
        if (claim.parent != null && claim.inheritParent) {
            // first check subdivision context
            value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
            // check parent context
            contexts.remove(claim.getContext());
            contexts.add(claim.parent.getContext());
            value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        } else {
            // check parent
            value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        // If source context is not null, check with mod id this time
        if (sourceContext != null) {
            contexts = new HashSet<>();
            String modId = sourceContext.getValue().split(":")[0];
            Context modContext = GriefPrevention.CUSTOM_CONTEXTS.get(modId);
            if (modContext != null) {
                contexts.add(modContext);
            }
            contexts.add(claim.getContext());
            if (claim.parent != null && claim.inheritParent) {
                // first check subdivision context
                value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
                if (value != Tristate.UNDEFINED) {
                    return value;
                }
                // check parent context
                contexts.remove(claim.getContext());
                contexts.add(claim.parent.getContext());
                value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
                if (value != Tristate.UNDEFINED) {
                    return value;
                }
            } else {
                // check parent
                value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
                if (value != Tristate.UNDEFINED) {
                    return value;
                }
            }
        }

        return getFlagDefaultPermission(claim, permission);
    }

    private static Tristate getFlagDefaultPermission(Claim claim, String permission) {
        // Fallback to defaults
        Set<Context> contexts = new HashSet<>();
        if (claim.isAdminClaim()) {
            contexts.add(GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            contexts.add(GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
        } else { // wilderness
            contexts.add(GriefPrevention.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
        }

        contexts.add(claim.world.getContext());
        // check persisted/transient default data
        Tristate value = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
        if (value != Tristate.UNDEFINED) {
            return value;
        }

        return Tristate.UNDEFINED;
    }

    public static Tristate getFlagOverride(Claim claim, String permission) {
        // First check for claim flag overrides
        if (!claim.isWildernessClaim()) {
            Set<Context> contexts = new HashSet<>();
            if (claim.isAdminClaim()) {
                contexts.add(GriefPrevention.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT);
            } else {
                contexts.add(GriefPrevention.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT);
            }

            contexts.add(claim.world.getContext());
            Tristate override = GriefPrevention.GLOBAL_SUBJECT.getPermissionValue(contexts, permission);
            if (override != Tristate.UNDEFINED) {
                return override;
            }
        }

        return Tristate.UNDEFINED;
    }

    @SuppressWarnings("deprecation")
    public static String getPermissionIdentifier(Object obj) {
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
                if (targetEntity instanceof Living) {
                    for (EnumCreatureType type : EnumCreatureType.values()) {
                        if (SpongeImplHooks.isCreatureOfType(mcEntity, type)) {
                            String[] parts = targetId.split(":");
                            targetId =  parts[0] + "." + GPFlags.SPAWN_TYPES.inverse().get(type) + "." + parts[1];
                            break;
                        }
                    }
                }

                if (targetEntity instanceof Item) {
                    targetId = ((Item) targetEntity).getItemType().getId();
                }

                if (!targetId.equals(UNKNOWN_CONTEXT) && GriefPrevention.CUSTOM_CONTEXTS.get(targetId) == null) {
                    GriefPrevention.CUSTOM_CONTEXTS.put(targetId, new Context("gp_source", targetId));
                }

                return targetId.toLowerCase();
            } else if (obj instanceof BlockType) {
                String targetId = ((BlockType) obj).getId();
                if (!targetId.equals(UNKNOWN_CONTEXT) && GriefPrevention.CUSTOM_CONTEXTS.get(targetId) == null) {
                    GriefPrevention.CUSTOM_CONTEXTS.put(targetId, new Context("gp_source", targetId));
                }

                return targetId;
            } else if (obj instanceof BlockState) {
                BlockState targetBlock = (BlockState) obj;
                Block mcBlock = (net.minecraft.block.Block) targetBlock.getType();
                String targetId = targetBlock.getType().getId() + "." + mcBlock.getMetaFromState((IBlockState) targetBlock);//.replace("[", ".[");
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

                if (!targetId.equals(UNKNOWN_CONTEXT) && GriefPrevention.CUSTOM_CONTEXTS.get(targetId) == null) {
                    GriefPrevention.CUSTOM_CONTEXTS.put(targetId, new Context("gp_source", targetId));
                }

                return targetId.toLowerCase();
            } else if (obj instanceof ItemType) {
                String targetId = ((ItemType) obj).getId().toLowerCase();
                if (!targetId.equals(UNKNOWN_CONTEXT) && GriefPrevention.CUSTOM_CONTEXTS.get(targetId) == null) {
                    GriefPrevention.CUSTOM_CONTEXTS.put(targetId, new Context("gp_source", targetId));
                }

                return targetId;
            } else if (obj instanceof String) {
                return obj.toString().toLowerCase();
            } else if (obj instanceof PluginContainer) {
                return ((PluginContainer) obj).getId();
            }
        }

        return "";
    }
}
