package me.ryanhamshire.GriefPrevention.configuration;

import com.google.common.reflect.TypeToken;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.Functional;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class GriefPreventionConfig<T extends GriefPreventionConfig.ConfigBase> {

    public enum Type {
        GLOBAL(GlobalConfig.class),
        DIMENSION(DimensionConfig.class),
        WORLD(WorldConfig.class);

        private final Class<? extends ConfigBase> clazz;

        Type(Class<? extends ConfigBase> type) {
            this.clazz = type;
        }
    }

    public static final String HEADER = "12.1.7\n"
            + "# If you need help with the configuration or have any questions related to GriefPrevention,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "# IRC: #spongedev @ irc.esper.net ( http://webchat.esper.net/?channel=spongedev )\n"
            + "# Forums: https://forums.spongepowered.org/\n";

    private Type type;
    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(HEADER));
    private ObjectMapper<T>.BoundInstance configMapper;
    private T configBase;

    public static final String CONFIG_ENABLED = "config-enabled";

    // GLOBAL
    public static final String GLOBAL_MONITOR_SPAM = "monitor-spam";

    // CLAIM
    public static final String CLAIM_MODULE_ENABLED = "enable-claims";
    public static final String CLAIM_ACCESSTRUST_COMMANDS = "accesstrust-commands";
    public static final String CLAIM_BANNED_ITEM_IDS = "banned-item-ids";
    public static final String CLAIM_ABANDON_RETURN_RATIO = "abandon-return-ratio";
    public static final String CLAIM_AUTO_RADIUS = "auto-claim-radius";
    public static final String CLAIM_BLOCKS_ACCRUED_PER_HOUR = "blocks-accrued-per-hour";
    public static final String CLAIM_DELIVER_MANUALS = "deliver-manuals";
    public static final String CLAIM_DAYS_INACTIVE_EXPIRATION = "expiration-all-claim-days";
    public static final String CLAIM_EXTEND_INTO_GROUND_DISTANCE = "extend-into-ground-distance";
    public static final String CLAIM_CHEST_DAYS = "chest-claim-days";
    public static final String CLAIM_UNUSED_DAYS = "unused-claim-days";
    public static final String CLAIM_AUTO_NATURE_RESTORE = "auto-nature-restore";
    public static final String CLAIM_MAX_DEPTH = "claim-max-depth";
    public static final String CLAIM_INVESTIGATION_TOOL = "investigation-tool";
    public static final String CLAIM_MAX_ACCRUED_BLOCKS = "max-accrued-blocks";
    public static final String CLAIM_MAX_PER_PLAYER = "max-claims-per-player";
    public static final String CLAIM_MINIMUM_AREA = "minimum-area";
    public static final String CLAIM_MINIMUM_WIDTH = "minimum-width";
    public static final String CLAIM_MODIFICATION_TOOL = "modification-tool";
    public static final String CLAIM_MODE = "claims-mode";
    public static final String CLAIM_FIRE_SPREADS_OUTSIDE = "fire-spreads-outside";
    public static final String CLAIM_ALWAYS_IGNORE_CLAIMS = "always-ignore-claims";
    public static final String CLAIM_IGNORED_ENTITY_IDS = "ignored-entity-ids";

    // DATABASE
    public static final String DATABASE_PASSWORD = "password";
    public static final String DATABASE_USERNAME = "username";
    public static final String DATABASE_PORT = "port";
    public static final String DATABASE_URL = "url";

    // ECONOMY
    public static final String ECONOMY_CLAIM_BLOCK_COST = "claim-block-cost";
    public static final String ECONOMY_CLAIM_BLOCK_SELL = "claim-block-sell";

    // GENERAL
    public static final String GENERAL_ADMIN_SIGN_NOTIFICATIONS = "admin-sign-notifications";
    public static final String GENERAL_ADMIN_WHISPER_NOTIFICATIONS = "admin-whisper-notifications";
    public static final String GENERAL_INITIAL_CLAIM_BLOCKS = "initial-claim-blocks";
    public static final String GENERAL_LIMIT_PISTONS_TO_CLAIMS = "limit-pistons-to-claims";
    public static final String GENERAL_LIMIT_SKY_TREES = "limit-sky-trees";
    public static final String GENERAL_LIMIT_TREE_GROWTH = "limit-tree-growth";
    public static final String GENERAL_MAX_PLAYERS_PER_IP = "max-players-per-ip";
    public static final String GENERAL_SMART_BAN = "smart-ban";
    public static final String GENERAL_ADMIN_WHISPERS = "admin-whispers";
    public static final String GENERAL_ADMIN_WHISPER_COMMANDS = "admin-whisper-commands";
    public static final String GENERAL_BANNED_ITEMS = "banned-items";

    // LOGGING
    public static final String LOGGING_DAYS_TO_KEEP = "days-stored";
    public static final String LOGGING_ADMIN_ACTIVITY = "admin-activity";
    public static final String LOGGING_DEBUG = "debug";
    public static final String LOGGING_SOCIAL_ACTIVITY = "social-acitivity";
    public static final String LOGGING_SUSPICIOUS_ACTIVITY = "suspicious-activity";

    // PVP
    public static final String PVP_PROTECT_ITEM_DROPS_DEATH = "protect-item-drops-death";
    public static final String PVP_PROTECT_ITEM_DROPS_DEATH_NONPVP = "protect-item-drops-death-non-pvp";
    public static final String PVP_ALLOW_COMBAT_ITEM_DROPS = "allow-combat-item-drops";
    public static final String PVP_COMBAT_TIMEOUT = "combat-timeout";
    public static final String PVP_PROTECT_FRESH_SPAWNS = "protect-fresh-spawns";
    public static final String PVP_PROTECT_PLAYERS_IN_CLAIMS = "protect-players-in-claims";
    public static final String PVP_PROTECT_PLAYERS_IN_ADMINCLAIMS = "protect-players-in-adminclaims";
    public static final String PVP_PROTECT_PLAYERS_IN_SUBDIVISIONS = "protect-players-in-subdivisions";
    public static final String PVP_PUNISH_LOGOUT = "punish-logout";
    public static final String PVP_RULES_ENABLED = "rules-enabled";
    public static final String PVP_BLOCKED_COMMANDS = "blocked-commands";

    // SIEGE
    public static final String SIEGE_ENABLED = "enable-sieges";
    public static final String SIEGE_BREAKABLE_BLOCKS = "breakable-blocks";
    public static final String SIEGE_WINNER_ACCESSIBLE_BLOCKS = "winner-accessible-blocks";

    // SPAM
    public static final String SPAM_MONITOR_ENABLED = "enable-spam-monitor";
    public static final String SPAM_LOGIN_COOLDOWN = "login-cooldown";
    public static final String SPAM_AUTO_BAN_OFFENDERS = "autoban-offenders";
    public static final String SPAM_MONITOR_COMMANDS = "monitor-commands";
    public static final String SPAM_BAN_MESSAGE = "ban-message";
    public static final String SPAM_BAN_WARNING_MESSAGE = "ban-warning-message";
    public static final String SPAM_ALLOWED_IPS = "allowed-ips";
    public static final String SPAM_DEATH_MESSAGE_COOLDOWN = "death-message-cooldown";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public GriefPreventionConfig(Type type, Path path) {

        this.type = type;

        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(this.type.clazz).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public T getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            this.configMapper.serialize(this.root.getNode(GriefPrevention.MOD_ID));
            this.loader.save(this.root);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults()
                    .setSerializers(
                            TypeSerializers.getDefaultSerializers().newChild().registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer()))
                    .setHeader(HEADER));
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPrevention.MOD_ID));
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
        }
    }

    public CompletableFuture<CommentedConfigurationNode> updateSetting(String key, Object value) {
        return Functional.asyncFailableFuture(() -> {
            CommentedConfigurationNode upd = getSetting(key);
            upd.setValue(value);
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPrevention.MOD_ID));
            this.loader.save(this.root);
            return upd;
        }, ForkJoinPool.commonPool());
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(GriefPrevention.MOD_ID);
    }

    public CommentedConfigurationNode getSetting(String key) {
        if (!key.contains(".") || key.indexOf('.') == key.length() - 1) {
            return null;
        } else {
            String category = key.substring(0, key.indexOf('.'));
            String prop = key.substring(key.indexOf('.') + 1);
            return getRootNode().getNode(category).getNode(prop);
        }
    }

    public Type getType() {
        return this.type;
    }

    public static class ConfigBase {
        @Setting
        public ClaimCategory claim = new ClaimCategory();
        @Setting
        public EconomyCategory economy = new EconomyCategory();
        @Setting
        public GeneralCategory general = new GeneralCategory();
        @Setting
        public PvpCategory pvp = new PvpCategory();
        @Setting
        public SiegeCategory siege = new SiegeCategory();
    }

    public static class GlobalConfig extends ConfigBase {
        @Setting
        public DatabaseCategory database = new DatabaseCategory();
        @Setting
        public LoggingCategory logging = new LoggingCategory();
        @Setting
        public SpamCategory spam = new SpamCategory();
    }

    public static class DimensionConfig extends ConfigBase {
        @Setting(
                value = CONFIG_ENABLED,
                comment = "Enabling config will override Global.")
        public boolean configEnabled = false;
    }

    public static class WorldConfig extends ConfigBase {
        @Setting(
                value = CONFIG_ENABLED,
                comment = "Enabling config will override Dimension and Global.")
        public boolean configEnabled = false;
    }

    @ConfigSerializable
    public static class ClaimCategory extends Category {
        @Setting(value = CLAIM_BANNED_ITEM_IDS, comment = "Contains list of banned item ids on server.")
        public List<String> bannedItemIds = new ArrayList<>();
        @Setting(value = CLAIM_ABANDON_RETURN_RATIO, comment = "The portion of claim blocks returned to a player when a claim is abandoned.")
        public double abandonReturnRatio = 1.0;
        @Setting(value = CLAIM_ACCESSTRUST_COMMANDS, comment = "The list of slashcommands requiring access trust when in a claim.")
        public List<String> accessTrustCommands = new ArrayList<>();
        @Setting(value = CLAIM_MODULE_ENABLED, comment = "Allows claims to be used.")
        public boolean allowClaims = true;
        @Setting(value = CLAIM_AUTO_RADIUS, comment = "Radius used for auto-created claims")
        public int claimRadius = 4;
        @Setting(value = CLAIM_BLOCKS_ACCRUED_PER_HOUR, comment = "Blocks earned per hour.")
        public int claimBlocksEarned = 100;
        @Setting(value = CLAIM_DELIVER_MANUALS, comment = "Send players manuals on claim creation.")
        public boolean deliverManuals = false;
        @Setting(value = CLAIM_DAYS_INACTIVE_EXPIRATION, comment = "How many days of inactivity before a player loses his claims.")
        public int daysInactiveClaimExpiration = 0;
        @Setting(value = CLAIM_CHEST_DAYS, comment = "Number of days of inactivity before an automatic chest claim will be deleted.")
        public int daysInactiveChestClaimExpiration = 7;
        @Setting(value = CLAIM_UNUSED_DAYS, comment = "Number of days of inactivity before an unused claim will be deleted.")
        public int daysInactiveUnusedClaimExpiration = 14;
        @Setting(value = CLAIM_AUTO_NATURE_RESTORE, comment = "Whether survival claims will be automatically restored to nature when auto-deleted.")
        public boolean claimAutoNatureRestore = false;
        @Setting(value = CLAIM_EXTEND_INTO_GROUND_DISTANCE, comment = "How far below the shoveled block a new claim will reach.")
        public int extendIntoGroundDistance = 5;
        @Setting(value = CLAIM_MAX_DEPTH, comment = "Limit on how deep claims can go.")
        public int maxClaimDepth = 0;
        @Setting(value = CLAIM_INVESTIGATION_TOOL, comment = "The item used to investigate claims with a right-click.")
        public String investigationTool = "minecraft:stick";
        @Setting(value = CLAIM_MAX_ACCRUED_BLOCKS, comment = "The limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks.")
        public int maxAccruedBlocks = 80000;
        @Setting(value = CLAIM_MAX_PER_PLAYER, comment = "Maximum number of claims per player.")
        public int maxClaimsPerPlayer = 0;
        @Setting(value = CLAIM_MINIMUM_AREA, comment = "Minimum area for non-admin claims.")
        public int claimMinimumArea = 100;
        @Setting(value = CLAIM_MINIMUM_WIDTH, comment = "Minimum width for non-admin claims.")
        public int claimMinimumWidth = 5;
        @Setting(value = CLAIM_MODIFICATION_TOOL, comment = "The item used to create/resize claims with a right click.")
        public String modificationTool = "minecraft:golden_shovel";
        @Setting(value = CLAIM_MODE, comment = "The mode used when creating claims. (0 = Disabled, 1 = Survival, 2 = SurvivalRequiringClaims, 3 = Creative)")
        public int claimMode = 1;
        @Setting(value = CLAIM_FIRE_SPREADS_OUTSIDE, comment = "Whether fire spreads outside of claims.")
        public boolean fireSpreadOutsideClaim = false;
        @Setting(value = CLAIM_ALWAYS_IGNORE_CLAIMS, comment = "List of player uuid's which ALWAYS ignore claims.")
        public List<String> alwaysIgnoreClaimsList = new ArrayList<>();
        @Setting(value = CLAIM_IGNORED_ENTITY_IDS, comment = "List of entity id's that ignore protection.")
        public List<String> ignoredEntityIds = new ArrayList<>();
    }

    @ConfigSerializable
    public static class DatabaseCategory extends Category {
        @Setting(value = DATABASE_PASSWORD, comment = "password")
        public String dbPassword = "";
        @Setting(value = DATABASE_USERNAME, comment = "username")
        public String dbUsername = "";
        @Setting(value = DATABASE_URL, comment = "url")
        public String dbURL = "";
    }

    @ConfigSerializable
    public static class EconomyCategory extends Category {
        @Setting(value = ECONOMY_CLAIM_BLOCK_COST, comment = "Cost to purchase a claim block. set to zero to disable purchase.")
        public double economyClaimBlockCost = 0;
        @Setting(value = ECONOMY_CLAIM_BLOCK_SELL, comment = "Return on a sold claim block. set to zero to disable sale.")
        public double economyClaimBlockSell = 0;
    }

    @ConfigSerializable
    public static class GeneralCategory extends Category {
        @Setting(value = GENERAL_ADMIN_SIGN_NOTIFICATIONS, comment = "Enable sign notifications for admins.")
        public boolean generalAdminSignNotifications = false;
        @Setting(value = GENERAL_ADMIN_WHISPER_NOTIFICATIONS, comment = "Enable whisper notifications for admins.")
        public boolean generalAdminWhisperNotifications = false;
        @Setting(value = GENERAL_INITIAL_CLAIM_BLOCKS, comment = "The number of claim blocks a new player starts with.")
        public int claimInitialBlocks = 100;
        @Setting(value = GENERAL_LIMIT_PISTONS_TO_CLAIMS, comment = "Whether pistons are limited to only move blocks located within the piston's land claim.")
        public boolean limitPistonsToClaims = false;
        @Setting(value = GENERAL_LIMIT_SKY_TREES, comment = "Whether players can build trees on platforms in the sky.")
        public boolean allowSkyTrees = true;
        @Setting(value = GENERAL_LIMIT_TREE_GROWTH, comment = "Whether trees should be prevented from growing into a claim from outside.")
        public boolean limitTreeGrowh = false;
        @Setting(value = GENERAL_MAX_PLAYERS_PER_IP, comment = "How many players can share an IP address.")
        public int sharedIpLimit = 3;
        @Setting(value = GENERAL_SMART_BAN, comment = "Whether to ban accounts which very likely owned by a banned player.")
        public boolean smartBan = false;
        @Setting(value = GENERAL_ADMIN_WHISPERS, comment = "Whether whispered messages will broadcast to administrators in game.")
        public boolean broadcastWhisperedMessagesToAdmins = false;
        @Setting(value = GENERAL_ADMIN_WHISPER_COMMANDS, comment = "List of whisper commands to eavesdrop on.")
        public List<String> whisperCommandList = new ArrayList<>();
        @Setting(value = GENERAL_BANNED_ITEMS, comment = "List of item id's banned on server.")
        public ArrayList<String> bannedItemList = new ArrayList<>();
    }

    @ConfigSerializable
    public static class LoggingCategory extends Category {

        @Setting(value = LOGGING_DAYS_TO_KEEP, comment = "How many days to keep logs in storage.")
        public int loggingDaysToKeep = 7;
        @Setting(value = LOGGING_ADMIN_ACTIVITY, comment = "Log admin activity.")
        public boolean loggingAdminActivity = false;
        @Setting(value = LOGGING_SOCIAL_ACTIVITY, comment = "Log social activity.")
        public boolean loggingSocialActions = false;
        @Setting(value = LOGGING_SUSPICIOUS_ACTIVITY, comment = "Log suspicious activity.")
        public boolean loggingSuspiciousActivity = false;
        @Setting(value = LOGGING_DEBUG, comment = "Enable debug logging.")
        public boolean loggingDebug = false;
    }

    @ConfigSerializable
    public static class PvpCategory extends Category {
        @Setting(value = PVP_RULES_ENABLED, comment = "Whether or not pvp anti-grief rules apply.")
        public boolean rulesEnabled = true;
        @Setting(value = PVP_PROTECT_ITEM_DROPS_DEATH, comment = "Whether player's dropped on death items are protected in pvp worlds.")
        public boolean protectItemsOnDeathPvp = false;
        @Setting(value = PVP_PROTECT_ITEM_DROPS_DEATH_NONPVP, comment = "Whether players' dropped on death items are protected in non-pvp worlds.")
        public boolean protectItemsOnDeathNonPvp = true;
        @Setting(value = PVP_ALLOW_COMBAT_ITEM_DROPS, comment = "Whether a player can drop items during combat to hide them.")
        public boolean allowCombatItemDrops = false;
        @Setting(value = PVP_COMBAT_TIMEOUT, comment = "How long combat is considered to continue after the most recent damage.")
        public int combatTimeout = 15;
        @Setting(value = PVP_PROTECT_FRESH_SPAWNS, comment = "Whether to make newly spawned players immune until they pick up an item.")
        public boolean protectFreshSpawns = true;
        @Setting(value = PVP_PROTECT_PLAYERS_IN_CLAIMS, comment = "Whether players may fight in player-owned land claims.")
        public boolean protectPlayersInClaims = false;
        @Setting(value = PVP_PROTECT_PLAYERS_IN_ADMINCLAIMS, comment = "Whether players may fight in admin-owned land claims.")
        public boolean protectPlayersInAdminClaims = false;
        @Setting(value = PVP_PROTECT_PLAYERS_IN_SUBDIVISIONS, comment = "Whether players may fight in subdivisions of admin-owned land claims.")
        public boolean protectPlayersInAdminSubClaims = false;
        @Setting(value = PVP_PUNISH_LOGOUT, comment = "Whether to kill players who log out during PvP combat.")
        public boolean punishPvpLogout = true;
        @Setting(value = PVP_BLOCKED_COMMANDS, comment = "Commands blocks from being used during PvP combat.")
        public List<String> blockedCommandList = new ArrayList<>();
    }

    @ConfigSerializable
    public static class SiegeCategory extends Category {
        @Setting(value = SIEGE_ENABLED, comment = "Whether sieges are allowed or not.")
        public boolean siegeEnabled = true;
        @Setting(value = SIEGE_BREAKABLE_BLOCKS, comment = "which blocks will be breakable in siege mode.")
        public List<String> breakableSiegeBlocks = new ArrayList<String>(
                Arrays.asList(ItemTypes.SAND.getId(), ItemTypes.GRAVEL.getId(), ItemTypes.GRASS.getId(),
                              ItemTypes.TALLGRASS.getId(), ItemTypes.GLASS.getId(), ItemTypes.DYE.getId(),
                              ItemTypes.SNOW.getId(), ItemTypes.STAINED_GLASS.getId(), ItemTypes.COBBLESTONE.getId()));
        @Setting(value = SIEGE_WINNER_ACCESSIBLE_BLOCKS, comment = "which blocks the siege winner can access in the loser's claim.")
        public List<String> winnerAccessibleBlocks = new ArrayList<String>(
                Arrays.asList(ItemTypes.ACACIA_DOOR.getId(), ItemTypes.ACACIA_FENCE.getId(), ItemTypes.ACACIA_FENCE_GATE.getId(),
                              ItemTypes.BIRCH_DOOR.getId(), ItemTypes.BIRCH_FENCE.getId(), ItemTypes.BIRCH_FENCE_GATE.getId(),
                              ItemTypes.DARK_OAK_DOOR.getId(), ItemTypes.DARK_OAK_FENCE.getId(), ItemTypes.DARK_OAK_FENCE_GATE.getId(),
                              ItemTypes.FENCE.getId(), ItemTypes.FENCE_GATE.getId(), ItemTypes.IRON_DOOR.getId(),
                              ItemTypes.IRON_TRAPDOOR.getId(), ItemTypes.WOODEN_DOOR.getId(), ItemTypes.STONE_BUTTON.getId(),
                              ItemTypes.WOODEN_BUTTON.getId(), ItemTypes.HEAVY_WEIGHTED_PRESSURE_PLATE.getId(), ItemTypes.LIGHT_WEIGHTED_PRESSURE_PLATE.getId(),
                              ItemTypes.LEVER.getId()));
    }

    @ConfigSerializable
    public static class SpamCategory extends Category {
        @Setting(value = SPAM_MONITOR_ENABLED, comment = "Whether or not to monitor for spam.")
        public boolean monitorEnabled = true;
        @Setting(value = SPAM_LOGIN_COOLDOWN, comment = "How long players must wait between logins. combats login spam.")
        public int loginCooldown = 60;
        @Setting(value = SPAM_AUTO_BAN_OFFENDERS, comment = "Whether or not to ban spammers automatically.")
        public boolean autoBanOffenders = false;
        @Setting(value = SPAM_MONITOR_COMMANDS, comment = "the list of slash commands monitored for spam,")
        public List<String> monitoredCommandList = new ArrayList<>();
        @Setting(value = SPAM_BAN_MESSAGE, comment = "Message to show an automatically banned player.")
        public String banMessage = "Banned for spam.";
        @Setting(value = SPAM_BAN_MESSAGE, comment = "Message to show a player who is close to spam level.")
        public String banWarningMessage = "Please reduce your noise level.  Spammers will be banned.";
        @Setting(value = SPAM_ALLOWED_IPS, comment = "IP addresses which will not be censored.")
        public List<String> allowedIpAddresses = new ArrayList<>();
        @Setting(value = SPAM_DEATH_MESSAGE_COOLDOWN, comment = "Cooldown period for death messages (per player) in seconds.")
        public int deathMessageCooldown = 60;
    }

    @ConfigSerializable
    private static class Category {
    }
}
