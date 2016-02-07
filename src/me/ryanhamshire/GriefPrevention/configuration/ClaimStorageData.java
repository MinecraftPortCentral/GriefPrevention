package me.ryanhamshire.GriefPrevention.configuration;

import com.google.common.collect.Lists;
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
import java.util.HashMap;
import java.util.List;
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

    public static HashMap<String, Object> flags = new HashMap<>();
    
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

    public static final String FLAGS_BLOCK_PLACE = "block-place";
    public static final String FLAGS_BLOCK_BREAK = "block-break";
    public static final String FLAGS_INTERACT_PRIMARY = "interact-primary";
    public static final String FLAGS_INTERACT_SECONDARY = "interact-secondary";
    public static final String FLAGS_INVENTORY = "inventory";
    public static final String FLAGS_EXPLOSIONS = "explosions";
    public static final String FLAGS_IGNITE = "ignite";
    public static final String FLAGS_MOB_BLOCK_DAMAGE = "mob-block-damage";
    public static final String FLAGS_MOB_PLAYER_DAMAGE = "mob-player-damage";
    public static final String FLAGS_MOB_RIDING = "mob-riding";
    public static final String FLAGS_ITEM_DROP = "item-drop";
    public static final String FLAGS_ITEM_PICKUP = "item-pickup";
    public static final String FLAGS_ITEM_USE = "item-use";
    public static final String FLAGS_PORTAL_USE = "portal-use";
    public static final String FLAGS_PVP = "pvp";
    public static final String FLAGS_SPAWN_MONSTERS = "spawn-monsters";
    public static final String FLAGS_SPAWN_PASSIVES = "spawn-passives";
    public static final String FLAGS_SPAWN_AMBIENTS = "spawn-ambient";
    public static final String FLAGS_SPAWN_AQUATICS = "spawn-aquatic";
    public static final String FLAGS_SPAWN_ANY = "spawn-any";
    public static final String FLAGS_SLEEP = "sleep";
    public static final String FLAGS_WATER_FLOW = "water-flow";
    public static final String FLAGS_LAVA_FLOW = "lava-flow";
    public static final String FLAGS_FIRE_SPREAD = "fire-spread";
    public static final String FLAGS_BLOCK_COMMANDS = "block-commands";
    public static final String FLAGS_PROJECTILES_PLAYER = "projectiles-player";
    public static final String FLAGS_PROJECTILES_MONSTER = "projectiles-monster";
    public static final String FLAGS_PROJECTILES_ANY = "projectiles-any";
    public static final String FLAGS_FORCE_DENY_ALL = "force-deny-all";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path) {
        initDefaultValues();
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

    public void initDefaultValues() {
        ClaimStorageData.flags.put(FLAGS_BLOCK_BREAK, false);
        ClaimStorageData.flags.put(FLAGS_BLOCK_COMMANDS, Lists.newArrayList());
        ClaimStorageData.flags.put(FLAGS_BLOCK_PLACE, false);
        ClaimStorageData.flags.put(FLAGS_EXPLOSIONS, false);
        ClaimStorageData.flags.put(FLAGS_FIRE_SPREAD, false);
        ClaimStorageData.flags.put(FLAGS_INTERACT_PRIMARY, false);
        ClaimStorageData.flags.put(FLAGS_INTERACT_SECONDARY, false);
        ClaimStorageData.flags.put(FLAGS_INVENTORY, false);
        ClaimStorageData.flags.put(FLAGS_ITEM_DROP, true);
        ClaimStorageData.flags.put(FLAGS_ITEM_PICKUP, true);
        ClaimStorageData.flags.put(FLAGS_ITEM_USE, true);
        ClaimStorageData.flags.put(FLAGS_MOB_BLOCK_DAMAGE, false);
        ClaimStorageData.flags.put(FLAGS_MOB_PLAYER_DAMAGE, true);
        ClaimStorageData.flags.put(FLAGS_MOB_RIDING, true);
        ClaimStorageData.flags.put(FLAGS_PORTAL_USE, false);
        ClaimStorageData.flags.put(FLAGS_PROJECTILES_PLAYER, false);
        ClaimStorageData.flags.put(FLAGS_PROJECTILES_MONSTER, true);
        ClaimStorageData.flags.put(FLAGS_PROJECTILES_ANY, true);
        ClaimStorageData.flags.put(FLAGS_PVP, false);
        ClaimStorageData.flags.put(FLAGS_SPAWN_MONSTERS, true);
        ClaimStorageData.flags.put(FLAGS_SPAWN_PASSIVES, true);
        ClaimStorageData.flags.put(FLAGS_SPAWN_AMBIENTS, true);
        ClaimStorageData.flags.put(FLAGS_SPAWN_AQUATICS, true);
        ClaimStorageData.flags.put(FLAGS_SPAWN_ANY, true);
        ClaimStorageData.flags.put(FLAGS_SLEEP, true);
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
        @Setting(value = FLAGS_BLOCK_PLACE, comment = "Allow/deny placing blocks.")
        public boolean blockPlace = (boolean) ClaimStorageData.flags.get(FLAGS_BLOCK_PLACE);
        @Setting(value = FLAGS_BLOCK_BREAK, comment = "Allow/deny breaking blocks.")
        public boolean blockBreak = (boolean) ClaimStorageData.flags.get(FLAGS_BLOCK_BREAK);
        @Setting(value = FLAGS_INTERACT_PRIMARY, comment = "Allow/deny left-clicking.")
        public boolean interactPrimary = (boolean) ClaimStorageData.flags.get(FLAGS_INTERACT_PRIMARY);
        @Setting(value = FLAGS_INTERACT_SECONDARY, comment = "Allow/deny right-clicking.")
        public boolean interactSecondary = (boolean) ClaimStorageData.flags.get(FLAGS_INTERACT_SECONDARY);
        @Setting(value = FLAGS_INVENTORY, comment = "Allow/deny blocks with inventories.")
        public boolean inventory = (boolean) ClaimStorageData.flags.get(FLAGS_INVENTORY);
        @Setting(value = FLAGS_EXPLOSIONS, comment = "Allow/deny explosions.")
        public boolean explosions = (boolean) ClaimStorageData.flags.get(FLAGS_EXPLOSIONS);
        @Setting(value = FLAGS_MOB_BLOCK_DAMAGE, comment = "Allow/deny mob block damage.")
        public boolean mobBlockDamage = (boolean) ClaimStorageData.flags.get(FLAGS_MOB_BLOCK_DAMAGE);
        @Setting(value = FLAGS_MOB_PLAYER_DAMAGE, comment = "Allow/deny mob player damage.")
        public boolean mobPlayerDamage = (boolean) ClaimStorageData.flags.get(FLAGS_MOB_PLAYER_DAMAGE);
        @Setting(value = FLAGS_MOB_RIDING, comment = "Allow/deny mob riding.")
        public boolean mobRiding = (boolean) ClaimStorageData.flags.get(FLAGS_MOB_RIDING);
        @Setting(value = FLAGS_ITEM_DROP, comment = "Allow/deny item drops.")
        public boolean itemDrop = (boolean) ClaimStorageData.flags.get(FLAGS_ITEM_DROP);
        @Setting(value = FLAGS_ITEM_PICKUP, comment = "Allow/deny picking up items.")
        public boolean itemPickup = (boolean) ClaimStorageData.flags.get(FLAGS_ITEM_PICKUP);
        @Setting(value = FLAGS_ITEM_USE, comment = "Allow/deny item use.")
        public boolean itemUse = (boolean) ClaimStorageData.flags.get(FLAGS_ITEM_USE);
        @Setting(value = FLAGS_PORTAL_USE, comment = "Allow/deny portal use.")
        public boolean portalUse = (boolean) ClaimStorageData.flags.get(FLAGS_PORTAL_USE);
        @Setting(value = FLAGS_PVP, comment = "Allow/deny pvp.")
        public boolean pvp = (boolean) ClaimStorageData.flags.get(FLAGS_PVP);
        @Setting(value = FLAGS_SPAWN_MONSTERS, comment = "Allow/deny the spawning of monsters.")
        public boolean spawnMonsters = (boolean) ClaimStorageData.flags.get(FLAGS_SPAWN_MONSTERS);
        @Setting(value = FLAGS_SPAWN_PASSIVES, comment = "Allow/deny the spawning of passive mobs.")
        public boolean spawnPassives = (boolean) ClaimStorageData.flags.get(FLAGS_SPAWN_PASSIVES);
        @Setting(value = FLAGS_SPAWN_AMBIENTS, comment = "Allow/deny the spawning of ambient mobs.")
        public boolean spawnAmbient = (boolean) ClaimStorageData.flags.get(FLAGS_SPAWN_AMBIENTS);
        @Setting(value = FLAGS_SPAWN_AQUATICS, comment = "Allow/deny the spawning of aquatic mobs.")
        public boolean spawnAquatic = (boolean) ClaimStorageData.flags.get(FLAGS_SPAWN_AQUATICS);
        @Setting(value = FLAGS_SPAWN_ANY, comment = "Allow/deny the spawning of any mobs.")
        public boolean spawnAny = (boolean) ClaimStorageData.flags.get(FLAGS_SPAWN_ANY);
        @Setting(value = FLAGS_SLEEP, comment = "Allow/deny sleep.")
        public boolean sleep = (boolean) ClaimStorageData.flags.get(FLAGS_SLEEP);
        @Setting(value = FLAGS_WATER_FLOW, comment = "Allow/deny water flow.")
        public boolean waterFlow = (boolean) ClaimStorageData.flags.get(FLAGS_WATER_FLOW);
        @Setting(value = FLAGS_LAVA_FLOW, comment = "Allow/deny lava flow.")
        public boolean lavaFlow = (boolean) ClaimStorageData.flags.get(FLAGS_LAVA_FLOW);
        @Setting(value = FLAGS_FIRE_SPREAD, comment = "Allow/deny fire spread.")
        public boolean fireSpread = (boolean) ClaimStorageData.flags.get(FLAGS_FIRE_SPREAD);
        @SuppressWarnings("unchecked")
        @Setting(value = FLAGS_BLOCK_COMMANDS, comment = "Blocked commands.")
        public List<String> blockCommands = (List<String>) ClaimStorageData.flags.get(FLAGS_BLOCK_COMMANDS);
        @Setting(value = FLAGS_PROJECTILES_PLAYER, comment = "Allow/deny player projectiles.")
        public boolean projectilesPlayer = (boolean) ClaimStorageData.flags.get(FLAGS_PROJECTILES_PLAYER);
        @Setting(value = FLAGS_PROJECTILES_MONSTER, comment = "Allow/deny monster projectiles.")
        public boolean projectilesMonster = (boolean) ClaimStorageData.flags.get(FLAGS_PROJECTILES_MONSTER);
        @Setting(value = FLAGS_PROJECTILES_ANY, comment = "Allow/deny any projectiles.")
        public boolean projectilesAny = (boolean) ClaimStorageData.flags.get(FLAGS_PROJECTILES_ANY);
        @Setting(value = FLAGS_FORCE_DENY_ALL, comment = "Only intended if you want to explicitly ignore all checking for player permissions.")
        public boolean forceDenyAll = false;
    }

    @ConfigSerializable
    private static class Category {
    }
}
