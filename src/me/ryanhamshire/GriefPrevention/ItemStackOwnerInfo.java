package me.ryanhamshire.GriefPrevention;

import java.util.UUID;
import org.bukkit.Location;

//tracks an item stack's owner.  ownership is limited to a locality due to problems with hash overlaps overprotecting item stacks
class ItemStackOwnerInfo
{
    public UUID ownerID;
    public Location locality;
    
    public ItemStackOwnerInfo(UUID ownerID, Location location)
    {
        this.ownerID = ownerID;
        this.locality = location;
    }
}
