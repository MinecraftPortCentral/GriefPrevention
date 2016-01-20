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
import org.spongepowered.api.util.Functional;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    public static final String FLAGS_ACTION_PLACE = "action-block-place";
    public static final String FLAGS_ACTION_BREAK = "action-block-break";
    public static final String FLAGS_ACTION_INTERACT = "action-interact";
    public static final String FLAGS_ACTION_INVENTORY_ACCESS = "action-inventory-access";
    public static final String FLAGS_EXPLOSIONS = "explosions";
    public static final String FLAGS_MOB_DAMAGE = "mob-damage";
    public static final String FLAGS_ITEM_DROP = "item-drop";
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
    public static final String FLAGS_PLAYER_PROJECTILES = "player-projectiles";
    public static final String FLAGS_MONSTER_PROJECTILES = "monster-projectiles";
    public static final String FLAGS_ANY_PROJECTILES = "any-projectiles";
    public static final String FLAGS_IGNORE_BLOCK_CHANGES = "ignore-block-changes";
    public static final String FLAGS_USE_PORTALS = "use-portals";

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
        @Setting(value = FLAGS_EXPLOSIONS, comment = "Explosions can break blocks.")
        public boolean explosions = false;
        @Setting(value = FLAGS_MOB_DAMAGE, comment = "Mobs can damage players.")
        public boolean mobDamage = true;
        @Setting(value = FLAGS_SPAWN_MONSTERS, comment = "Monsters can spawn.")
        public boolean spawnMonsters = true;
        @Setting(value = FLAGS_SPAWN_PASSIVES, comment = "Passives can spawn.")
        public boolean spawnPassives = true;
        @Setting(value = FLAGS_SPAWN_AMBIENTS, comment = "Ambients can spawn.")
        public boolean spawnAmbients = true;
        @Setting(value = FLAGS_SPAWN_AQUATICS, comment = "Aquatics can spawn.")
        public boolean spawnAquatics = true;
        @Setting(value = FLAGS_SLEEP, comment = "Players can sleep in beds.")
        public boolean sleepInBeds = true;
        @Setting(value = FLAGS_ITEM_DROP, comment = "Items can drop.")
        public boolean itemDrops = true;
        @Setting(value = FLAGS_WATER_FLOW, comment = "Water can flow.")
        public boolean waterFlow = true;
        @Setting(value = FLAGS_LAVA_FLOW, comment = "Lava can flow.")
        public boolean lavaFlow = true;
    }

    @ConfigSerializable
    private static class Category {
    }
}
