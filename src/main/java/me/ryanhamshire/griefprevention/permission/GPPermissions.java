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

public class GPPermissions {

    // Claims
    public static final String COMMAND_ABANDON_CLAIM = "griefprevention.user.claim.command.abandon";
    public static final String COMMAND_ABANDON_ALL_CLAIMS = "griefprevention.user.claim.command.abandon-all";
    public static final String COMMAND_ABANDON_TOP_LEVEL_CLAIM = "griefprevention.user.claim.command.abandon-top-level";
    public static final String COMMAND_CUBOID_CLAIMS = "griefprevention.user.claim.command.cuboid";
    public static final String COMMAND_LIST_CLAIMS = "griefprevention.user.claim.command.list";
    public static final String COMMAND_BASIC_MODE = "griefprevention.user.claim.command.basic-mode";
    public static final String COMMAND_GIVE_BOOK = "griefprevention.user.claim.command.give.book";
    public static final String COMMAND_GIVE_PET = "griefprevention.user.claim.command.give.pet";
    public static final String COMMAND_CLAIM_INFO_OTHERS = "griefprevention.user.claim.command.info.others";
    public static final String COMMAND_CLAIM_INFO_BASE = "griefprevention.user.claim.command.info.base";
    public static final String COMMAND_CLAIM_INFO_TELEPORT_OTHERS = "griefprevention.user.claim.command.info.teleport.others";
    public static final String COMMAND_CLAIM_INFO_TELEPORT_BASE = "griefprevention.user.claim.command.info.teleport.base";
    public static final String COMMAND_CLAIM_SPAWN = "griefprevention.user.claim.command.spawn";
    public static final String COMMAND_CLAIM_SET_SPAWN = "griefprevention.user.claim.command.set-spawn";
    public static final String COMMAND_SET_CLAIM_NAME = "griefprevention.user.claim.command.name";
    public static final String COMMAND_SET_CLAIM_FAREWELL = "griefprevention.user.claim.command.farewell";
    public static final String COMMAND_SET_CLAIM_GREETING = "griefprevention.user.claim.command.greeting";
    public static final String COMMAND_SUBDIVIDE_CLAIMS = "griefprevention.user.claim.command.subdivide-mode";
    public static final String COMMAND_TRANSFER_CLAIM = "griefprevention.user.claim.command.transfer";
    public static final String COMMAND_BUY_CLAIM_BLOCKS = "griefprevention.user.claim.command.buy-blocks";
    public static final String COMMAND_SELL_CLAIM_BLOCKS = "griefprevention.user.claim.command.sell-blocks";
    public static final String COMMAND_LIST_CLAIM_FLAGS = "griefprevention.user.claim.command.list-flags";
    public static final String COMMAND_BAN_ITEM = "griefprevention.user.claim.command.ban-item";
    public static final String COMMAND_UNBAN_ITEM = "griefprevention.user.claim.command.unban-item";
    public static final String COMMAND_SIEGE = "griefprevention.user.claim.command.siege";
    public static final String COMMAND_SUBDIVISION_INHERIT = "griefprevention.user.claim.command.inherit";
    public static final String CLAIM_CREATE = "griefprevention.user.claim.create";
    public static final String CLAIM_CUBOID_BASIC = "griefprevention.user.claim.cuboid.basic";
    public static final String CLAIM_CUBOID_SUBDIVISION = "griefprevention.user.claim.cuboid.subdivision";
    public static final String CLAIM_RESIZE = "griefprevention.user.claim.resize";
    public static final String SIEGE_IMMUNE = "griefprevention.user.claim.siege.immune";
    public static final String VISUALIZE_CLAIMS = "griefprevention.user.claim.visualize";
    public static final String VISUALIZE_CLAIMS_NEARBY = "griefprevention.user.claim.visualize.nearby";
    public static final String COMMAND_PLAYER_INFO_BASE = "griefprevention.user.command.info.base";
    public static final String COMMAND_PLAYER_INFO_OTHERS = "griefprevention.user.command.info.others";

    // flags
    public static final String USER_CLAIM_FLAGS = "griefprevention.user.claim.flag";
    public static final String COMMAND_FLAGS_CLAIM = "griefprevention.user.claim.command.flag.base";
    public static final String COMMAND_FLAGS_DEBUG = "griefprevention.user.claim.command.flag.debug";
    public static final String COMMAND_FLAGS_PLAYER = "griefprevention.user.claim.command.flag.player";
    public static final String COMMAND_FLAGS_GROUP = "griefprevention.user.claim.command.flag.group";
    public static final String COMMAND_FLAGS_RESET = "griefprevention.user.claim.command.flag.reset";

    // Admin
    public static final String COMMAND_ADJUST_CLAIM_BLOCKS = "griefprevention.admin.claim.command.adjust-claim-blocks";
    public static final String COMMAND_ADMIN_CLAIMS = "griefprevention.admin.claim.command.admin-mode";
    public static final String COMMAND_CLAIM_CLEAR = "griefprevention.admin.claim.command.clear";
    public static final String COMMAND_CLAIM_PERMISSION_GROUP = "griefprevention.admin.claim.command.permission-group";
    public static final String COMMAND_CLAIM_PERMISSION_PLAYER = "griefprevention.admin.claim.command.permission-player";
    public static final String COMMAND_DEBUG = "griefprevention.admin.claim.command.debug";
    public static final String COMMAND_DELETE_CLAIM_BASE = "griefprevention.admin.claim.command.delete.base";
    public static final String DELETE_CLAIM_BASIC = "griefprevention.admin.claim.command.delete.basic";
    public static final String DELETE_CLAIM_ADMIN = "griefprevention.admin.claim.command.delete.admin";
    public static final String COMMAND_DELETE_CLAIMS = "griefprevention.admin.claim.command.delete-claims";
    public static final String COMMAND_DELETE_ADMIN_CLAIMS = "griefprevention.admin.command.delete-admin-claims";
    public static final String COMMAND_LIST_ADMIN_CLAIMS = "griefprevention.admin.claim.command.list.admin";
    public static final String COMMAND_SET_ACCRUED_CLAIM_BLOCKS = "griefprevention.admin.command.set-accrued-claim-blocks";
    public static final String COMMAND_RESTORE_NATURE = "griefprevention.admin.command.restore-nature.base";
    public static final String COMMAND_RESTORE_NATURE_AGGRESSIVE = "griefprevention.admin.command.restore-nature.aggressive";
    public static final String COMMAND_RESTORE_NATURE_FILL = "griefprevention.admin.command.restore-nature.fill";
    public static final String COMMAND_RELOAD = "griefprevention.admin.command.reload";
    public static final String SET_ADMIN_FLAGS = "griefprevention.admin.claim.set-admin-flags";
    public static final String LIST_BASIC_CLAIMS = "griefprevention.admin.claim.list.basic";
    public static final String ADMIN_CLAIM_FLAGS = "griefprevention.admin.claim.flag";
    public static final String MANAGE_FLAG_DEFAULTS = "griefprevention.admin.flag-defaults";
    public static final String MANAGE_FLAG_OVERRIDES = "griefprevention.admin.flag-overrides";
    public static final String MANAGE_WILDERNESS = "griefprevention.admin.claim.wilderness";
    public static final String MANAGE_PERMISSION_OPTIONS = "griefprevention.admin.claim.manage.options";
    public static final String COMMAND_IGNORE_CLAIMS = "griefprevention.admin.claim.command.ignore.base";
    public static final String IGNORE_CLAIMS_BASIC = "griefprevention.admin.claim.command.ignore.basic";
    public static final String IGNORE_CLAIMS_ADMIN = "griefprevention.admin.claim.command.ignore.admin";
    public static final String IGNORE_CLAIMS_WILDERNESS = "griefprevention.admin.claim.command.ignore.wilderness";
    public static final String CLAIM_CUBOID_ADMIN = "griefprevention.admin.claim.cuboid";
    public static final String CLAIM_RESIZE_ADMIN = "griefprevention.admin.claim.resize.admin";
    public static final String CLAIM_RESIZE_ADMIN_SUBDIVISION = "griefprevention.admin.claim.resize.admin.subdivision";
    public static final String CLAIM_RESIZE_BASIC = "griefprevention.admin.claim.resize.basic";
    public static final String CLAIM_RESIZE_BASIC_SUBDIVISION = "griefprevention.admin.claim.resize.basic.subdivision";
    public static final String OVERRIDE_CLAIM_RESIZE = "griefprevention.admin.claim.override.resize";
    public static final String OVERRIDE_CLAIM_LIMIT = "griefprevention.admin.claim.override.limit";
    public static final String EAVES_DROP_SIGNS = "griefprevention.admin.eavesdrop.signs";
    public static final String COMMAND_UNLOCK_DROPS = "griefprevention.admin.command.unlock-drops";
    public static final String NO_PVP_IMMUNITY = "griefprevention.admin.no-pvp-immunity";

    // Chat
    public static final String COMMAND_IGNORE_PLAYER = "griefprevention.user.chat.command.ignore";
    public static final String COMMAND_UNIGNORE_PLAYER = "griefprevention.user.chat.command.unignore";
    public static final String COMMAND_LIST_IGNORED_PLAYERS = "griefprevention.admin.chat.command.list";
    public static final String COMMAND_SEPARATE_PLAYERS = "griefprevention.admin.chat.command.separate";
    public static final String COMMAND_UNSEPARATE_PLAYERS = "griefprevention.admin.chat.command.unseparate";
    public static final String COMMAND_SOFT_MUTE_PLAYER = "griefprevention.admin.chat.command.softmute";
    public static final String NOT_IGNORABLE = "griefprevention.admin.chat.not-ignorable";
    public static final String SPAM = "griefprevention.admin.chat.spam-override";
    public static final String EAVES_DROP = "griefprevention.admin.chat.eavesdrop";

    // Misc
    public static final String COMMAND_HELP = "griefprevention.user.command.help";

    // Trust
    public static final String COMMAND_GIVE_ACCESS_TRUST = "griefprevention.user.claim.command.trust.access";
    public static final String COMMAND_GIVE_CONTAINER_TRUST = "griefprevention.user.claim.command.trust.container";
    public static final String COMMAND_GIVE_BUILDER_TRUST = "griefprevention.user.claim.command.trust.build";
    public static final String COMMAND_GIVE_PERMISSION_TRUST = "griefprevention.user.claim.command.trust.permission";
    public static final String COMMAND_LIST_TRUST = "griefprevention.user.claim.command.trust.list";
    public static final String COMMAND_REMOVE_TRUST = "griefprevention.user.claim.command.trust.remove";
    public static final String COMMAND_TRUST_ALL = "griefprevention.user.claim.command.trust.all";

    // Flags
    public static final String BLOCK_BREAK = "griefprevention.flag.block-break";
    public static final String BLOCK_PLACE = "griefprevention.flag.block-place";
    public static final String COMMAND_EXECUTE = "griefprevention.flag.command-execute";
    public static final String COMMAND_EXECUTE_PVP = "griefprevention.flag.command-execute-pvp";
    public static final String ENTER_CLAIM = "griefprevention.flag.enter-claim";
    public static final String ENTITY_COLLIDE_BLOCK = "griefprevention.flag.collide-block";
    public static final String ENTITY_COLLIDE_ENTITY = "griefprevention.flag.collide-entity";
    public static final String ENTITY_DAMAGE = "griefprevention.flag.entity-damage";
    public static final String ENTITY_EXPLOSION = "griefprevention.flag.explosion";
    public static final String ENTITY_FALL = "griefprevention.flag.entity-fall";
    public static final String ENTITY_RIDING = "griefprevention.flag.entity-riding";
    public static final String ENTITY_SPAWN = "griefprevention.flag.entity-spawn";
    public static final String ENTITY_TELEPORT_FROM = "griefprevention.flag.entity-teleport-from";
    public static final String ENTITY_TELEPORT_TO = "griefprevention.flag.entity-teleport-to";
    public static final String EXIT_CLAIM = "griefprevention.flag.exit-claim";
    public static final String EXPLOSION = "griefprevention.flag.explosion";
    public static final String EXPLOSION_SURFACE = "griefprevention.flag.explosion-surface";
    public static final String FIRE_SPREAD = "griefprevention.flag.fire-spread";
    public static final String FLAG_BASE = "griefprevention.flag";
    public static final String LIQUID_FLOW = "griefprevention.flag.liquid-flow";
    public static final String INTERACT_BLOCK_PRIMARY = "griefprevention.flag.interact-block-primary";
    public static final String INTERACT_BLOCK_SECONDARY = "griefprevention.flag.interact-block-secondary";
    public static final String INTERACT_ENTITY_PRIMARY = "griefprevention.flag.interact-entity-primary";
    public static final String INTERACT_ENTITY_SECONDARY = "griefprevention.flag.interact-entity-secondary";
    public static final String INTERACT_INVENTORY = "griefprevention.flag.interact-inventory";
    public static final String INTERACT_ITEM_PRIMARY = "griefprevention.flag.interact-item-primary";
    public static final String INTERACT_ITEM_SECONDARY = "griefprevention.flag.interact-item-secondary";
    public static final String ITEM_DROP = "griefprevention.flag.item-drop";
    public static final String ITEM_PICKUP = "griefprevention.flag.item-pickup";
    public static final String ITEM_SPAWN = "griefprevention.flag.item-spawn";
    public static final String ITEM_USE = "griefprevention.flag.item-use";
    public static final String PORTAL_USE = "griefprevention.flag.portal-use";
    public static final String PROJECTILE_IMPACT_BLOCK = "griefprevention.flag.projectile-impact-block";
    public static final String PROJECTILE_IMPACT_ENTITY = "griefprevention.flag.projectile-impact-entity";
}
