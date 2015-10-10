package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

class PendingItemProtection {

    public Location<World> location;
    public UUID owner;
    long expirationTimestamp;
    ItemStack itemStack;

    public PendingItemProtection(Location<World> location, UUID owner, long expirationTimestamp, ItemStack itemStack) {
        this.location = location;
        this.owner = owner;
        this.expirationTimestamp = expirationTimestamp;
        this.itemStack = itemStack;
    }
}
