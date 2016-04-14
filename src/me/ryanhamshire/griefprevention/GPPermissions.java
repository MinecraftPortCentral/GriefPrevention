/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

public class GPPermissions {

    // Claims
    public static final String CREATE_CLAIM = "griefprevention.claims.create";
    public static final String ABANDON_CLAIM = "griefprevention.claims.abandon";
    public static final String ABANDON_ALL_CLAIMS = "griefprevention.claims.abandon-all";
    public static final String ABANDON_TOP_LEVEL_CLAIM = "griefprevention.claims.abandon-top-level";
    public static final String LIST_CLAIMS = "griefprevention.claims.list";
    public static final String CLAIMS_LIST_ADMIN = "griefprevention.claims.list-admin";
    public static final String CLAIMS_LIST_OTHER = "griefprevention.claims.list-other";
    public static final String DELETE_CLAIM = "griefprevention.claims.delete";
    public static final String DELETE_ALL_CLAIMS = "griefprevention.claims.delete-all";
    public static final String DELETE_ADMIN_CLAIM = "griefprevention.claims.delete-admin";
    public static final String CLAIM_MODE_BASIC = "griefprevention.claims.basic-mode";
    public static final String GIVE_CLAIM_BOOK = "griefprevention.claims.book";
    public static final String CLAIM_MANAGE_FLAGS = "griefprevention.claims.flag";
    public static final String GIVE_PET = "griefprevention.claims.givepet";
    public static final String CLAIM_INFO = "griefprevention.claims.info";
    public static final String RENAME_CLAIM = "griefprevention.claims.rename";
    public static final String SUBDIVIDE_CLAIMS = "griefprevention.claims.subdivide";
    public static final String TRANSFER_CLAIM = "griefprevention.claims.transfer";
    public static final String IGNORE_CLAIMS = "griefprevention.claims.ignore";
    public static final String TOGGLE_IGNORE_CLAIMS = "griefprevention.claims.toggle-ignore";
    public static final String VISUALIZE_NEARBY_CLAIMS = "griefprevention.claims.visualize";
    public static final String CLAIMS_ADMIN = "griefprevention.claims.admin";
    public static final String BUY_CLAIM_BLOCKS = "griefprevention.claims.buy-blocks";
    public static final String SELL_CLAIM_BLOCKS = "griefprevention.claims.sell-blocks";
    public static final String ADJUST_CLAIM_BLOCKS = "griefprevention.claims.adjust-blocks";
    public static final String SET_ACCRUED_CLAIM_BLOCKS = "griefprevention.claims.set-accrued-blocks";
    public static final String OVERRIDE_CLAIM_COUNT_LIMIT = "griefprevention.claims.limit-override";

    // Item bans
    public static final String BAN_ITEM = "griefprevention.items.ban";
    public static final String UNBAN_ITEM = "griefprevention.items.unban";
    public static final String LIST_ITEM_BANS = "griefprevention.items.list";
    public static final String IGNORE_ITEM_BANS = "griefprevention.items.ignore";

    // Trust
    public static final String GIVE_ACCESS_TRUST = "griefprevention.trust.access";
    public static final String GIVE_CONTAINER_TRUST = "griefprevention.trust.container";
    public static final String GIVE_PERMISSION_TRUST = "griefprevention.trust.permission";
    public static final String GIVE_FULL_TRUST = "griefprevention.trust.full";
    public static final String LIST_TRUST = "griefprevention.trust.list";
    public static final String REMOVE_TRUST = "griefprevention.trust.remove";

    // Players
    public static final String IGNORE_PLAYER = "griefprevention.players.ignore";
    public static final String UNIGNORE_PLAYER = "griefprevention.players.unignore";
    public static final String LIST_IGNORED_PLAYERS = "griefprevention.players.list";
    public static final String SEPARATE_PLAYERS = "griefprevention.players.separate";
    public static final String UNSEPARATE_PLAYERS = "griefprevention.players.unseparate";
    public static final String SOFT_MUTE_PLAYER = "griefprevention.players.softmute";

    // Restoration
    public static final String RESTORE_NATURE = "griefprevention.restore.nature";
    public static final String RESTORE_NATURE_AGGRESSIVE = "griefprevention.restore.nature-aggressive";
    public static final String RESTORE_NATURE_FILL = "griefprevention.restore.nature-fill";

    // Siege
    public static final String SIEGE = "griefprevention.siege";
    public static final String SIEGE_IMMUNE = "griefprevention.siege.immune";

    // EavesDrop
    public static final String EAVES_DROP = "griefprevention.eavesdrop";
    public static final String EAVES_DROP_SIGNS = "griefprevention.eavesdrop.signs";

    // Misc
    public static final String DEBUG = "griefprevention.debug";
    public static final String NO_PVP_IMMUNITY = "griefprevention.no-pvp-immunity";
    public static final String NOT_IGNORABLE = "griefprevention.not-ignorable";
    public static final String HELP = "griefprevention.help";
    public static final String RELOAD = "griefprevention.reload";
    public static final String SPAM = "griefprevention.spam";
    public static final String TRAPPED = "griefprevention.trapped";
    public static final String UNLOCK_DROPS = "griefprevention.unlock-drops";

    // Flags
    public static final String BLOCK_BREAK = "griefprevention.flag.block-break";
    public static final String BLOCK_COMMANDS = "griefprevention.flag.block-commands";
    public static final String BLOCK_PLACE = "griefprevention.flag.block-place";
    public static final String EXPLOSIONS = "griefprevention.flag.explosions";
    public static final String FIRE_SPREAD = "griefprevention.flag.fire-spread";
    public static final String FLAG_BASE = "griefprevention.flag";
    public static final String FORCE_DENY_ALL = "griefprevention.flag.force-deny-all";
    public static final String LAVA_FLOW = "griefprevention.flag.lava-flow";
    public static final String MOB_BLOCK_DAMAGE = "griefprevention.flag.mob-block-damage";
    public static final String MOB_PLAYER_DAMAGE = "griefprevention.flag.mob-player-damage";
    public static final String MOB_RIDING = "griefprevention.flag.mob-riding";
    public static final String INTERACT_PRIMARY = "griefprevention.flag.interact-primary";
    public static final String INTERACT_SECONDARY = "griefprevention.flag.interact-secondary";
    public static final String INTERACT_INVENTORY = "griefprevention.flag.interact-inventory";
    public static final String ITEM_DROP = "griefprevention.flag.item-drop";
    public static final String ITEM_PICKUP = "griefprevention.flag.item-pickup";
    public static final String ITEM_USE = "griefprevention.flag.item-use";
    public static final String PORTAL_USE = "griefprevention.flag.portal-use";
    public static final String PROJECTILES_ANY = "griefprevention.flag.projectiles-any";
    public static final String PROJECTILES_MONSTER = "griefprevention.flag.projectiles-monster";
    public static final String PROJECTILES_PLAYER = "griefprevention.flag.projectiles-player";
    public static final String PVP = "griefprevention.flag.pvp";
    public static final String SLEEP = "griefprevention.flag.sleep";
    public static final String SPAWN_AMBIENTS = "griefprevention.flag.spawn-ambients";
    public static final String SPAWN_ANY = "griefprevention.flag.spawn-any";
    public static final String SPAWN_AQUATICS = "griefprevention.flag.spawn-aquatics";
    public static final String SPAWN_MONSTERS = "griefprevention.flag.spawn-monsters";
    public static final String SPAWN_PASSIVES = "griefprevention.flag.spawn-passives";
    public static final String WATER_FLOW = "griefprevention.flag.water-flow";
    public static final String VILLAGER_TRADING = "griefprevention.flag.villager-trading";
}
