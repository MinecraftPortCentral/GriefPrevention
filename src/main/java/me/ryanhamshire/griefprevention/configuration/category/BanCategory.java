package me.ryanhamshire.griefprevention.configuration.category;

import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;

import java.util.Map;

@ConfigSerializable
public class BanCategory extends ConfigCategory {

    @Setting
    private Map<String, Text> banReasons = Maps.newHashMap();

    public void addBan(String permission, Text reason) {
        permission = permission.replace("griefprevention.flag.", "");
        this.banReasons.put(permission, reason);
    }

    public void removeBan(String permission) {
        this.banReasons.remove(permission);
    }

    public Text getReason(String permission) {
        permission = permission.replace("griefprevention.flag.", "");
        for (Map.Entry<String, Text> banEntry : this.banReasons.entrySet()) {
            if (permission.contains(banEntry.getKey())) {
                return banEntry.getValue();
            }
        }
        return null;
    }
}
