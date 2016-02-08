package me.ryanhamshire.GriefPrevention.configuration;

import com.google.common.collect.Maps;
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
import org.spongepowered.api.util.Functional;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class ClaimStorageData {

    public static final String HEADER = "12.1.7\n"
            + "# If you need help with the configuration or have any questions related to GriefPrevention,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "# IRC: #spongedev @ irc.esper.net ( http://webchat.esper.net/?channel=spongedev )\n"
            + "# Forums: https://forums.spongepowered.org/\n";

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(HEADER));
    private ObjectMapper<ClaimDataNode>.BoundInstance configMapper;
    private ClaimDataNode configBase;
    public Path filePath;

    // MAIN
    public static final String MAIN_WORLD_UUID = "world-uuid";
    public static final String MAIN_OWNER_UUID = "owner-uuid";
    public static final String MAIN_PARENT_CLAIM_UUID = "parent-claim-uuid";
    public static final String MAIN_LESSER_BOUNDARY_CORNER = "lesser-boundary-corner";
    public static final String MAIN_GREATER_BOUNDARY_CORNER = "greater-boundary-corner";
    public static final String MAIN_BUILDERS = "builders";
    public static final String MAIN_CONTAINERS = "containers";
    public static final String MAIN_ACCESSORS = "accessors";
    public static final String MAIN_MANAGERS = "managers";
    public static final String MAIN_PROTECTION_BLACKLIST = "bypass-protection-items";

    // FLAGS
    public static final String FLAGS_BLOCK_BREAK = "block-break";
    public static final String FLAGS_BLOCK_COMMANDS = "block-commands";
    public static final String FLAGS_BLOCK_NOTIFY = "block-notify";
    public static final String FLAGS_BLOCK_PLACE = "block-place";
    public static final String FLAGS_EXPLOSIONS = "explosions";
    public static final String FLAGS_FIRE_SPREAD = "fire-spread";
    public static final String FLAGS_FORCE_DENY_ALL = "force-deny-all";
    public static final String FLAGS_IGNITE = "ignite";
    public static final String FLAGS_INTERACT_PRIMARY = "interact-primary";
    public static final String FLAGS_INTERACT_SECONDARY = "interact-secondary";
    public static final String FLAGS_INVENTORY = "inventory";
    public static final String FLAGS_ITEM_DROP = "item-drop";
    public static final String FLAGS_ITEM_PICKUP = "item-pickup";
    public static final String FLAGS_ITEM_USE = "item-use";
    public static final String FLAGS_LAVA_FLOW = "lava-flow";
    public static final String FLAGS_MOB_BLOCK_DAMAGE = "mob-block-damage";
    public static final String FLAGS_MOB_PLAYER_DAMAGE = "mob-player-damage";
    public static final String FLAGS_MOB_RIDING = "mob-riding";
    public static final String FLAGS_PORTAL_USE = "portal-use";
    public static final String FLAGS_PROJECTILES_ANY = "projectiles-any";
    public static final String FLAGS_PROJECTILES_MONSTER = "projectiles-monster";
    public static final String FLAGS_PROJECTILES_PLAYER = "projectiles-player";
    public static final String FLAGS_PVP = "pvp";
    public static final String FLAGS_SLEEP = "sleep";
    public static final String FLAGS_SPAWN_AMBIENTS = "spawn-ambient";
    public static final String FLAGS_SPAWN_ANY = "spawn-any";
    public static final String FLAGS_SPAWN_AQUATICS = "spawn-aquatic";
    public static final String FLAGS_SPAWN_PASSIVES = "spawn-passives";
    public static final String FLAGS_SPAWN_MONSTERS = "spawn-monsters";
    public static final String FLAGS_WATER_FLOW = "water-flow";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path) {
        this.filePath = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataNode.class).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public ClaimDataNode getConfig() {
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

    @ConfigSerializable
    public static class ClaimDataNode {

        @Setting(value = MAIN_WORLD_UUID, comment = "The world uuid associated with claim.")
        public String worldUniqueId;
        @Setting(value = MAIN_OWNER_UUID, comment = "The owner uuid assocated with claim.")
        public String ownerUniqueId;
        @Setting(value = MAIN_LESSER_BOUNDARY_CORNER, comment = "The lesser boundary corner location of claim.")
        public String lesserBoundaryCornerPos;
        @Setting(value = MAIN_GREATER_BOUNDARY_CORNER, comment = "The greater boundary corner location of claim.")
        public String greaterBoundaryCornerPos;
        @Setting(value = MAIN_BUILDERS, comment = "The builders associated with claim.")
        public ArrayList<String> builders = new ArrayList<>();
        @Setting(value = MAIN_CONTAINERS, comment = "The containers associated with claim.")
        public ArrayList<String> containers = new ArrayList<>();
        @Setting(value = MAIN_ACCESSORS, comment = "The accessors associated with claim.")
        public ArrayList<String> accessors = new ArrayList<>();
        @Setting(value = MAIN_MANAGERS, comment = "The managers associated with claim.")
        public ArrayList<String> managers = new ArrayList<>();
        @Setting(value = MAIN_PARENT_CLAIM_UUID, comment = "The parent claim uuid of claim.")
        public String parentUniqueId;
        @Setting(value = MAIN_PROTECTION_BLACKLIST, comment = "Item id's that are not protected within claim.")
        public ArrayList<String> protectionBlacklist = new ArrayList<>();
        @Setting
        public ClaimDataFlagsCategory flags = new ClaimDataFlagsCategory();
    }

    @ConfigSerializable
    public static class ClaimDataFlagsCategory extends Category {

        @Setting(value = FLAGS_BLOCK_BREAK, comment = "Allow/deny breaking blocks.")
        public boolean blockBreak = false;
        @SuppressWarnings("unchecked")
        @Setting(value = FLAGS_BLOCK_COMMANDS, comment = "Blocked commands.")
        public List<String> blockCommands = new ArrayList<>();
        @Setting(value = FLAGS_BLOCK_NOTIFY, comment = "Allow/deny notifying blocks.")
        public boolean blockNotify = false;
        @Setting(value = FLAGS_BLOCK_PLACE, comment = "Allow/deny placing blocks.")
        public boolean blockPlace = false;
        @Setting(value = FLAGS_EXPLOSIONS, comment = "Allow/deny explosions.")
        public boolean explosions = false;
        @Setting(value = FLAGS_FIRE_SPREAD, comment = "Allow/deny fire spread.")
        public boolean fireSpread = false;
        @Setting(value = FLAGS_FORCE_DENY_ALL, comment = "Only intended if you want to explicitly ignore all checking for player permissions.")
        public boolean forceDenyAll = false;
        @Setting(value = FLAGS_INTERACT_PRIMARY, comment = "Allow/deny left-clicking.")
        public boolean interactPrimary = false;
        @Setting(value = FLAGS_INTERACT_SECONDARY, comment = "Allow/deny right-clicking.")
        public boolean interactSecondary = false;
        @Setting(value = FLAGS_INVENTORY, comment = "Allow/deny blocks with inventories.")
        public boolean inventory = false;
        @Setting(value = FLAGS_ITEM_DROP, comment = "Allow/deny item drops.")
        public boolean itemDrop = false;
        @Setting(value = FLAGS_ITEM_PICKUP, comment = "Allow/deny picking up items.")
        public boolean itemPickup = false;
        @Setting(value = FLAGS_ITEM_USE, comment = "Allow/deny item use.")
        public boolean itemUse = false;
        @Setting(value = FLAGS_LAVA_FLOW, comment = "Allow/deny lava flow.")
        public boolean lavaFlow = true;
        @Setting(value = FLAGS_MOB_BLOCK_DAMAGE, comment = "Allow/deny mob block damage.")
        public boolean mobBlockDamage = false;
        @Setting(value = FLAGS_MOB_PLAYER_DAMAGE, comment = "Allow/deny mob player damage.")
        public boolean mobPlayerDamage = false;
        @Setting(value = FLAGS_MOB_RIDING, comment = "Allow/deny mob riding.")
        public boolean mobRiding = false;
        @Setting(value = FLAGS_PORTAL_USE, comment = "Allow/deny portal use.")
        public boolean portalUse = true;
        @Setting(value = FLAGS_PROJECTILES_ANY, comment = "Allow/deny any projectiles.")
        public boolean projectilesAny = true;
        @Setting(value = FLAGS_PROJECTILES_MONSTER, comment = "Allow/deny monster projectiles.")
        public boolean projectilesMonster = true;
        @Setting(value = FLAGS_PROJECTILES_PLAYER, comment = "Allow/deny player projectiles.")
        public boolean projectilesPlayer = false;
        @Setting(value = FLAGS_PVP, comment = "Allow/deny pvp.")
        public boolean pvp = false;
        @Setting(value = FLAGS_SLEEP, comment = "Allow/deny sleep.")
        public boolean sleep = true;
        @Setting(value = FLAGS_SPAWN_AMBIENTS, comment = "Allow/deny the spawning of ambient mobs.")
        public boolean spawnAmbient = true;
        @Setting(value = FLAGS_SPAWN_ANY, comment = "Allow/deny the spawning of any mobs.")
        public boolean spawnAny = true;
        @Setting(value = FLAGS_SPAWN_AQUATICS, comment = "Allow/deny the spawning of aquatic mobs.")
        public boolean spawnAquatic = true;
        @Setting(value = FLAGS_SPAWN_MONSTERS, comment = "Allow/deny the spawning of monsters.")
        public boolean spawnMonsters = true;
        @Setting(value = FLAGS_SPAWN_PASSIVES, comment = "Allow/deny the spawning of passive mobs.")
        public boolean spawnPassives = true;
        @Setting(value = FLAGS_WATER_FLOW, comment = "Allow/deny water flow.")
        public boolean waterFlow = true;

        public Map<String, Object> getFlagMap() {
            Map<String, Object> flagMap = Maps.newHashMap();
            flagMap.put(FLAGS_BLOCK_BREAK, this.blockBreak);
            flagMap.put(FLAGS_BLOCK_COMMANDS, this.blockCommands);
            flagMap.put(FLAGS_BLOCK_NOTIFY, this.blockNotify);
            flagMap.put(FLAGS_BLOCK_PLACE, this.blockPlace);
            flagMap.put(FLAGS_EXPLOSIONS, this.explosions);
            flagMap.put(FLAGS_FIRE_SPREAD, this.fireSpread);
            flagMap.put(FLAGS_FORCE_DENY_ALL, this.forceDenyAll);
            flagMap.put(FLAGS_INTERACT_PRIMARY, this.interactPrimary);
            flagMap.put(FLAGS_INTERACT_SECONDARY, this.interactSecondary);
            flagMap.put(FLAGS_INVENTORY, this.inventory);
            flagMap.put(FLAGS_ITEM_DROP, this.itemDrop);
            flagMap.put(FLAGS_ITEM_PICKUP, this.itemPickup);
            flagMap.put(FLAGS_ITEM_USE, this.itemUse);
            flagMap.put(FLAGS_LAVA_FLOW, this.lavaFlow);
            flagMap.put(FLAGS_MOB_BLOCK_DAMAGE, this.mobBlockDamage);
            flagMap.put(FLAGS_MOB_PLAYER_DAMAGE, this.mobPlayerDamage);
            flagMap.put(FLAGS_MOB_RIDING, this.mobRiding);
            flagMap.put(FLAGS_PORTAL_USE, this.portalUse);
            flagMap.put(FLAGS_PROJECTILES_PLAYER, this.projectilesPlayer);
            flagMap.put(FLAGS_PROJECTILES_MONSTER, this.projectilesMonster);
            flagMap.put(FLAGS_PROJECTILES_ANY, this.projectilesAny);
            flagMap.put(FLAGS_PVP, this.pvp);
            flagMap.put(FLAGS_SPAWN_MONSTERS, this.spawnMonsters);
            flagMap.put(FLAGS_SPAWN_PASSIVES, this.spawnPassives);
            flagMap.put(FLAGS_SPAWN_AMBIENTS, this.spawnAmbient);
            flagMap.put(FLAGS_SPAWN_AQUATICS, this.spawnAquatic);
            flagMap.put(FLAGS_SPAWN_ANY, this.spawnAny);
            flagMap.put(FLAGS_SLEEP, this.sleep);
            flagMap.put(FLAGS_WATER_FLOW, this.waterFlow);
            return flagMap;
        }

        public void setFlagValue(String flag, Object value) {
            switch (flag) {
                case FLAGS_BLOCK_BREAK:
                    this.blockBreak = (boolean) value;
                    return;
                case FLAGS_BLOCK_COMMANDS:
                    this.blockCommands = (List<String>) value;
                    return;
                case FLAGS_BLOCK_NOTIFY:
                    this.blockNotify = (boolean) value;
                    return;
                case FLAGS_BLOCK_PLACE:
                    this.blockPlace = (boolean) value;
                    return;
                case FLAGS_EXPLOSIONS:
                    this.explosions = (boolean) value;
                    return;
                case FLAGS_FIRE_SPREAD:
                    this.fireSpread = (boolean) value;
                    return;
                case FLAGS_FORCE_DENY_ALL:
                    this.forceDenyAll = (boolean) value;
                    return;
                case FLAGS_INTERACT_PRIMARY:
                    this.interactPrimary = (boolean) value;
                    return;
                case FLAGS_INTERACT_SECONDARY:
                    this.interactSecondary = (boolean) value;
                    return;
                case FLAGS_INVENTORY:
                    this.inventory = (boolean) value;
                    return;
                case FLAGS_ITEM_DROP:
                    this.itemDrop = (boolean) value;
                    return;
                case FLAGS_ITEM_PICKUP:
                    this.itemPickup = (boolean) value;
                    return;
                case FLAGS_ITEM_USE:
                    this.itemUse = (boolean) value;
                    return;
                case FLAGS_LAVA_FLOW:
                    this.lavaFlow = (boolean) value;
                    return;
                case FLAGS_MOB_BLOCK_DAMAGE:
                    this.mobBlockDamage = (boolean) value;
                    return;
                case FLAGS_MOB_PLAYER_DAMAGE:
                    this.mobPlayerDamage = (boolean) value;
                    return;
                case FLAGS_MOB_RIDING:
                    this.mobRiding = (boolean) value;
                    return;
                case FLAGS_PORTAL_USE:
                    this.portalUse = (boolean) value;
                    return;
                case FLAGS_PROJECTILES_ANY:
                    this.projectilesAny = (boolean) value;
                    return;
                case FLAGS_PROJECTILES_MONSTER:
                    this.projectilesMonster = (boolean) value;
                    return;
                case FLAGS_PROJECTILES_PLAYER:
                    this.projectilesPlayer = (boolean) value;
                    return;
                case FLAGS_PVP:
                    this.pvp = (boolean) value;
                    return;
                case FLAGS_SLEEP:
                    this.sleep = (boolean) value;
                    return;
                case FLAGS_SPAWN_AMBIENTS:
                    this.spawnAmbient = (boolean) value;
                    return;
                case FLAGS_SPAWN_ANY:
                    this.spawnAny = (boolean) value;
                    return;
                case FLAGS_SPAWN_AQUATICS:
                    this.spawnAquatic = (boolean) value;
                    return;
                case FLAGS_SPAWN_MONSTERS:
                    this.spawnMonsters = (boolean) value;
                    return;
                case FLAGS_SPAWN_PASSIVES:
                    this.spawnPassives = (boolean) value;
                    return;
                case FLAGS_WATER_FLOW:
                    this.waterFlow = (boolean) value;
                    return;
                default:
                    return;
            }
        }

        public Object getFlagValue(String flag) {
            switch (flag) {
                case FLAGS_BLOCK_BREAK:
                    return this.blockBreak;
                case FLAGS_BLOCK_COMMANDS:
                    return this.blockCommands;
                case FLAGS_BLOCK_NOTIFY:
                    return this.blockNotify;
                case FLAGS_BLOCK_PLACE:
                    return this.blockPlace;
                case FLAGS_EXPLOSIONS:
                    return this.explosions;
                case FLAGS_FIRE_SPREAD:
                    return this.fireSpread;
                case FLAGS_FORCE_DENY_ALL:
                    return this.forceDenyAll;
                case FLAGS_INTERACT_PRIMARY:
                    return this.interactPrimary;
                case FLAGS_INTERACT_SECONDARY:
                    return this.interactSecondary;
                case FLAGS_INVENTORY:
                    return this.inventory;
                case FLAGS_ITEM_DROP:
                    return this.itemDrop;
                case FLAGS_ITEM_PICKUP:
                    return this.itemPickup;
                case FLAGS_ITEM_USE:
                    return this.itemUse;
                case FLAGS_LAVA_FLOW:
                    return this.lavaFlow;
                case FLAGS_MOB_BLOCK_DAMAGE:
                    return this.mobBlockDamage;
                case FLAGS_MOB_PLAYER_DAMAGE:
                    return this.mobPlayerDamage;
                case FLAGS_MOB_RIDING:
                    return this.mobRiding;
                case FLAGS_PORTAL_USE:
                    return this.portalUse;
                case FLAGS_PROJECTILES_ANY:
                    return this.projectilesAny;
                case FLAGS_PROJECTILES_MONSTER:
                    return this.projectilesMonster;
                case FLAGS_PROJECTILES_PLAYER:
                    return this.projectilesPlayer;
                case FLAGS_PVP:
                    return this.pvp;
                case FLAGS_SLEEP:
                    return this.sleep;
                case FLAGS_SPAWN_AMBIENTS:
                    return this.spawnAmbient;
                case FLAGS_SPAWN_ANY:
                    return this.spawnAny;
                case FLAGS_SPAWN_AQUATICS:
                    return this.spawnAquatic;
                case FLAGS_SPAWN_MONSTERS:
                    return this.spawnMonsters;
                case FLAGS_SPAWN_PASSIVES:
                    return this.spawnPassives;
                case FLAGS_WATER_FLOW:
                    return this.waterFlow;
                default:
                    return null;
            }
        }
    }

    @ConfigSerializable
    private static class Category {

    }
}
