/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

//handles events related to entities
class EntityEventHandler implements Listener
{
	//convenience reference for the singleton datastore
	private DataStore dataStore;
	
	public EntityEventHandler(DataStore dataStore)
	{
		this.dataStore = dataStore;
	}
	
	//don't allow endermen to change blocks
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityChangeBLock(EntityChangeBlockEvent event)
	{
		if(!GriefPrevention.instance.config_endermenMoveBlocks && event.getEntityType() == EntityType.ENDERMAN)
		{
			event.setCancelled(true);
		}
		
		else if(!GriefPrevention.instance.config_silverfishBreakBlocks && event.getEntityType() == EntityType.SILVERFISH)
		{
			event.setCancelled(true);
		}
		
		//don't allow the wither to break blocks, when the wither is determined, too expensive to constantly check for claimed blocks
		else if(event.getEntityType() == EntityType.WITHER && GriefPrevention.instance.config_claims_worldModes.get(event.getBlock().getWorld()) != ClaimsMode.Disabled)
		{
			event.setCancelled(true);
		}
	}
	
	//don't allow zombies to break down doors
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZombieBreakDoor(EntityBreakDoorEvent event)
	{		
		if(!GriefPrevention.instance.config_zombiesBreakDoors) event.setCancelled(true);
	}
	
	//don't allow entities to trample crops
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event)
	{
		if(!GriefPrevention.instance.config_creaturesTrampleCrops && event.getBlock().getType() == Material.SOIL)
		{
			event.setCancelled(true);
		}
	}
	
	//when an entity explodes...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent)
	{		
		List<Block> blocks = explodeEvent.blockList();
		Location location = explodeEvent.getLocation();
		
		//FEATURE: explosions don't destroy blocks when they explode near or above sea level in standard worlds
		boolean isCreeper = (explodeEvent.getEntity() != null && explodeEvent.getEntity() instanceof Creeper);
		
		//exception for some land claims in survival worlds, see notes below
		Claim originationClaim = null;
		if(!GriefPrevention.instance.creativeRulesApply(location))
        {
		    originationClaim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        }
		
		if( location.getWorld().getEnvironment() == Environment.NORMAL && GriefPrevention.instance.claimsEnabledForWorld(location.getWorld()) && ((isCreeper && GriefPrevention.instance.config_blockSurfaceCreeperExplosions) || (!isCreeper && GriefPrevention.instance.config_blockSurfaceOtherExplosions)))			
		{
			for(int i = 0; i < blocks.size(); i++)
			{
				Block block = blocks.get(i);
				if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
				
				//in survival worlds, if claim explosions are enabled for the source claim, allow non-creeper explosions to destroy blocks in and under that claim even above sea level. 
				if(!isCreeper && originationClaim != null && originationClaim.areExplosivesAllowed && originationClaim.contains(block.getLocation(), true, false)) continue;
				
				if(block.getLocation().getBlockY() > GriefPrevention.instance.getSeaLevel(location.getWorld()) - 7)
				{
					blocks.remove(i--);
				}
			}			
		}
		
		//special rule for creative worlds: explosions don't destroy anything
		if(GriefPrevention.instance.creativeRulesApply(explodeEvent.getLocation()))
		{
			for(int i = 0; i < blocks.size(); i++)
			{
				Block block = blocks.get(i);
				if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
				
				blocks.remove(i--);
			}
		}
		
		//FEATURE: explosions don't damage claimed blocks	
		Claim claim = null;
		for(int i = 0; i < blocks.size(); i++)  //for each destroyed block
		{
			Block block = blocks.get(i);
			if(block.getType() == Material.AIR) continue;  //if it's air, we don't care
			
			if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
			
			claim = this.dataStore.getClaimAt(block.getLocation(), false, claim); 
			//if the block is claimed, remove it from the list of destroyed blocks
			if(claim != null && !claim.areExplosivesAllowed)
			{
				blocks.remove(i--);
			}
		}
	}
	
	//when an item spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event)
	{
		//if in a creative world, cancel the event (don't drop items on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getLocation()))
		{
			event.setCancelled(true);
		}
		
		//if item is on watch list, apply protection
		ArrayList<PendingItemProtection> watchList = GriefPrevention.instance.pendingItemWatchList;
		Item newItem = event.getEntity();
		Long now = null;
		for(int i = 0; i < watchList.size(); i++)
		{
		    PendingItemProtection pendingProtection = watchList.get(i);
		    //ignore and remove any expired pending protections
            if(now == null) now = System.currentTimeMillis();
            if(pendingProtection.expirationTimestamp < now)
            {
                watchList.remove(i--);
                continue;
            }
		    //skip if item stack doesn't match
		    if(pendingProtection.itemStack.getAmount() != newItem.getItemStack().getAmount() ||
		       pendingProtection.itemStack.getType() != newItem.getItemStack().getType())
		    {
		        continue;
		    }
		    
		    //skip if new item location isn't near the expected spawn area 
		    Location spawn = event.getLocation();
		    Location expected = pendingProtection.location;
		    if(!spawn.getWorld().equals(expected.getWorld()) ||
		       spawn.getX() < expected.getX() - 5 ||
		       spawn.getX() > expected.getX() + 5 ||
		       spawn.getZ() < expected.getZ() - 5 ||
		       spawn.getZ() > expected.getZ() + 5 ||
		       spawn.getY() < expected.getY() - 15 ||
		       spawn.getY() > expected.getY() + 3)
		    {
		        continue;
		    }
		    
		    //otherwise, mark item with protection information
		    newItem.setMetadata("GP_ITEMOWNER", new FixedMetadataValue(GriefPrevention.instance, pendingProtection.owner));
		    
		    //and remove pending protection data
		    watchList.remove(i);
		    break;
		}
	}
	
	//when an experience bottle explodes...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onExpBottle(ExpBottleEvent event)
	{
		//if in a creative world, cancel the event (don't drop exp on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			event.setExperience(0);
		}
	}
	
	//when a creature spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntitySpawn(CreatureSpawnEvent event)
	{
		//these rules apply only to creative worlds
		if(!GriefPrevention.instance.creativeRulesApply(event.getLocation())) return;
		
		//chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
		SpawnReason reason = event.getSpawnReason();
		if(reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN && event.getEntityType() != EntityType.ARMOR_STAND)
		{
			event.setCancelled(true);
			return;
		}

		//otherwise, just apply the limit on total entities per claim (and no spawning in the wilderness!)
		Claim claim = this.dataStore.getClaimAt(event.getLocation(), false, null);
		if(claim == null || claim.allowMoreEntities() != null)
		{
			event.setCancelled(true);
			return;
		}
	}
	
	//when an entity dies...
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event)
	{
		LivingEntity entity = event.getEntity();
		
		//don't do the rest in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(entity.getWorld())) return;
		
		//special rule for creative worlds: killed entities don't drop items or experience orbs
		if(GriefPrevention.instance.creativeRulesApply(entity.getLocation()))
		{
			event.setDroppedExp(0);
			event.getDrops().clear();			
		}
		
		//FEATURE: when a player is involved in a siege (attacker or defender role)
		//his death will end the siege
		
		if(!(entity instanceof Player)) return;  //only tracking players
		
		Player player = (Player)entity;
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
		//if involved in a siege
		if(playerData.siegeData != null)
		{
			//don't drop items as usual, they will be sent to the siege winner
			event.getDrops().clear();
			
			//end it, with the dieing player being the loser
			this.dataStore.endSiege(playerData.siegeData, null, player.getName(), true /*ended due to death*/);
		}
		
		//FEATURE: lock dropped items to player who dropped them
        
        World world = entity.getWorld();
        
        //decide whether or not to apply this feature to this situation (depends on the world where it happens)
        boolean isPvPWorld = GriefPrevention.instance.config_pvp_enabledWorlds.contains(world);
        if((isPvPWorld && GriefPrevention.instance.config_lockDeathDropsInPvpWorlds) || 
           (!isPvPWorld && GriefPrevention.instance.config_lockDeathDropsInNonPvpWorlds))
        {
            //remember information about these drops so that they can be marked when they spawn as items
            long expirationTime = System.currentTimeMillis() + 3000;  //now + 3 seconds
            Location deathLocation = player.getLocation();
            UUID playerID = player.getUniqueId();
            List<ItemStack> drops = event.getDrops();
            for(ItemStack stack : drops)
            {
                GriefPrevention.instance.pendingItemWatchList.add(
                       new PendingItemProtection(deathLocation, playerID, expirationTime, stack));
            }
            
            //allow the player to receive a message about how to unlock any drops
            playerData.dropsAreUnlocked = false;
            playerData.receivedDropUnlockAdvertisement = false;
        }
	}
	
	//when an entity picks up an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPickup(EntityChangeBlockEvent event)
	{
		//FEATURE: endermen don't steal claimed blocks
		
		//if its an enderman
		if(event.getEntity() instanceof Enderman)
		{
			//and the block is claimed
			if(this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null) != null)
			{
				//he doesn't get to steal it
				event.setCancelled(true);
			}
		}
	}
	
	//when a painting is broken
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onHangingBreak(HangingBreakEvent event)
    {
	    //don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;
	    
	    //FEATURE: claimed paintings are protected from breakage
		
		//explosions don't destroy hangings
	    if(event.getCause() == RemoveCause.EXPLOSION)
	    {
	        event.setCancelled(true);
	        return;
	    }
	    
	    //only allow players to break paintings, not anything else (like water and explosions)
		if(!(event instanceof HangingBreakByEntityEvent))
    	{
        	event.setCancelled(true);
        	return;
    	}
        
        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent)event;
        
        //who is removing it?
		Entity remover = entityEvent.getRemover();
        
		//again, making sure the breaker is a player
		if(!(remover instanceof Player))
        {
        	event.setCancelled(true);
        	return;
        }
		
		//if the player doesn't have build permission, don't allow the breakage
		Player playerRemover = (Player)entityEvent.getRemover();
        String noBuildReason = GriefPrevention.instance.allowBuild(playerRemover, event.getEntity().getLocation(), Material.AIR);
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(playerRemover, TextMode.Err, noBuildReason);
        }
    }
	
	//when a painting is placed...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingPlace(HangingPlaceEvent event)
	{
	    //don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld())) return;
	    
	    //FEATURE: similar to above, placing a painting requires build permission in the claim
	
		//if the player doesn't have permission, don't allow the placement
		String noBuildReason = GriefPrevention.instance.allowBuild(event.getPlayer(), event.getEntity().getLocation(), Material.PAINTING);
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
			return;
        }
		
		//otherwise, apply entity-count limitations for creative worlds
		else if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
			Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, playerData.lastClaim);
			if(claim == null) return;
			
			String noEntitiesReason = claim.allowMoreEntities();
			if(noEntitiesReason != null)
			{
				GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noEntitiesReason);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	//when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamage (EntityDamageEvent event)
	{
		//monsters are never protected
		if(event.getEntity() instanceof Monster) return;
		
		//protect pets from environmental damage types which could be easily caused by griefers
        if(event.getEntity() instanceof Tameable && !GriefPrevention.instance.config_pvp_enabledWorlds.contains(event.getEntity().getWorld()))
        {
            Tameable tameable = (Tameable)event.getEntity();
            if(tameable.isTamed())
            {
                DamageCause cause = event.getCause();
                if( cause != null && (
                    cause == DamageCause.ENTITY_EXPLOSION ||
                    cause == DamageCause.FALLING_BLOCK ||
                    cause == DamageCause.FIRE ||
                    cause == DamageCause.FIRE_TICK ||
                    cause == DamageCause.LAVA ||
                    cause == DamageCause.SUFFOCATION))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
		
		//the rest is only interested in entities damaging entities (ignoring environmental damage)
        if(!(event instanceof EntityDamageByEntityEvent)) return;
		
		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
		
		//determine which player is attacking, if any
		Player attacker = null;
		Projectile arrow = null;
		Entity damageSource = subEvent.getDamager();
		
		if(damageSource != null)
		{
    		if(damageSource instanceof Player)
    		{
    			attacker = (Player)damageSource;
    		}
    		else if(damageSource instanceof Projectile)
    		{
    			arrow = (Projectile)damageSource;
    			if(arrow.getShooter() instanceof Player)
    			{
    				attacker = (Player)arrow.getShooter();
    			}
    		}
		}
		
		//if the attacker is a player and defender is a player (pvp combat)
		if(attacker != null && event.getEntity() instanceof Player && GriefPrevention.instance.config_pvp_enabledWorlds.contains(attacker.getWorld()))
		{
			//FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
			
			//doesn't apply when the attacker has the no pvp immunity permission
			//this rule is here to allow server owners to have a world with no spawn camp protection by assigning permissions based on the player's world
			if(attacker.hasPermission("griefprevention.nopvpimmunity")) return;
			
			Player defender = (Player)(event.getEntity());
			
			PlayerData defenderData = this.dataStore.getPlayerData(((Player)event.getEntity()).getUniqueId());
			PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
			
			//otherwise if protecting spawning players
			if(GriefPrevention.instance.config_pvp_protectFreshSpawns)
			{
				if(defenderData.pvpImmune)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
					return;
				}
				
				if(attackerData.pvpImmune)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}		
			}
			
			//FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
			if(GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims || GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims)
			{
				Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
				if(	attackerClaim != null && 
					(attackerClaim.isAdminClaim() && attackerClaim.parent == null && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims ||
					 attackerClaim.isAdminClaim() && attackerClaim.parent != null && GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions ||
					!attackerClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims))
				{
					attackerData.lastClaim = attackerClaim;
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}
				
				Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
				if( defenderClaim != null &&
					(defenderClaim.isAdminClaim() && defenderClaim.parent == null && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims ||
		             defenderClaim.isAdminClaim() && defenderClaim.parent != null && GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions ||
					!defenderClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims))
				{
					defenderData.lastClaim = defenderClaim;
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
					return;
				}
			}
			
			//FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
			//FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated
			
			long now = Calendar.getInstance().getTimeInMillis();
			defenderData.lastPvpTimestamp = now;
			defenderData.lastPvpPlayer = attacker.getName();
			attackerData.lastPvpTimestamp = now;
			attackerData.lastPvpPlayer = defender.getName();			
		}
		
		//FEATURE: protect claimed animals, boats, minecarts, and items inside item frames
		//NOTE: animals can be lead with wheat, vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		
		//if theft protection is enabled
		if(event instanceof EntityDamageByEntityEvent)
		{
		    //don't track in worlds where claims are not enabled
	        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;
	        
	        //if the damaged entity is a claimed item frame or armor stand, the damager needs to be a player with container trust in the claim
		    if(subEvent.getEntityType() == EntityType.ITEM_FRAME || subEvent.getEntityType() == EntityType.ARMOR_STAND)
		    {
		        //decide whether it's claimed
		        Claim cachedClaim = null;
                PlayerData playerData = null;
                if(attacker != null)
                {
                    playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    cachedClaim = playerData.lastClaim;
                }
                
                Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);
                
                //if it's claimed
                if(claim != null)
                {
                    //if attacker isn't a player, cancel
                    if(attacker == null)
                    {
                        event.setCancelled(true);
                        return;
                    }
                    
                    //otherwise player must have container trust in the claim
                    String failureReason = claim.allowContainers(attacker);
                    if(failureReason != null)
                    {
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(attacker, TextMode.Err, failureReason);
                        return;
                    }
                }
		    }
		    
		    //if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
			if ((subEvent.getEntity() instanceof Creature && GriefPrevention.instance.config_claims_protectCreatures))
			{
			    //if entity is tameable and has an owner, apply special rules
		        if(subEvent.getEntity() instanceof Tameable && !GriefPrevention.instance.config_pvp_enabledWorlds.contains(subEvent.getEntity().getWorld()))
		        {
		            Tameable tameable = (Tameable)subEvent.getEntity();
		            if(tameable.isTamed() && tameable.getOwner() != null)
		            {
		                //limit attacks by players to owners and admins in ignore claims mode
		                if(attacker != null)
		                {
    		                UUID ownerID = tameable.getOwner().getUniqueId();
    		               
    		                //if the player interacting is the owner, always allow
    		                if(attacker.getUniqueId().equals(ownerID)) return;
    		                
    		                //allow for admin override
    		                PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
    		                if(attackerData.ignoreClaims) return;
    		               
    		                //otherwise disallow in non-pvp worlds
    		                if(!GriefPrevention.instance.config_pvp_enabledWorlds.contains(subEvent.getEntity().getLocation().getWorld()))
                            {
        		                OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID); 
                                String ownerName = owner.getName();
        		                if(ownerName == null) ownerName = "someone";
        		                String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, ownerName);
        		                if(attacker.hasPermission("griefprevention.ignoreclaims"))
        		                    message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        		                GriefPrevention.sendMessage(attacker, TextMode.Err, message);
        		                event.setCancelled(true);
        		                return;
                            }
		                }
		            }
		        }
			    
			    Claim cachedClaim = null;
				PlayerData playerData = null;
				
				//if not a player or an explosive, allow
				if(attacker == null && damageSource != null && damageSource.getType() != EntityType.CREEPER && !(damageSource instanceof Explosive))
				{
				    return;
				}
				
				if(attacker != null)
				{
					playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
					cachedClaim = playerData.lastClaim;
				}
				
				Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false, cachedClaim);
				
				//if it's claimed
				if(claim != null)
				{
					//if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
					//why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
					if(attacker == null)
					{
						//exception case
						if(event.getEntity() instanceof Villager && damageSource != null && damageSource instanceof Monster && claim.isAdminClaim())
						{
							return;
						}
						
						//all other cases
						else
						{
							event.setCancelled(true);
						}						
					}
					
					//otherwise the player damaging the entity must have permission
					else
					{		
						String noContainersReason = claim.allowContainers(attacker);
						if(noContainersReason != null)
						{
							event.setCancelled(true);
							
							//kill the arrow to avoid infinite bounce between crowded together animals
							if(arrow != null) arrow.remove();
							
							String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
	                        if(attacker.hasPermission("griefprevention.ignoreclaims"))
	                            message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
	                        GriefPrevention.sendMessage(attacker, TextMode.Err, message);
	                        event.setCancelled(true);
						}
						
						//cache claim for later
						if(playerData != null)
						{
							playerData.lastClaim = claim;
						}						
					}
				}
			}
		}
	}
	
	//when a vehicle is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onVehicleDamage (VehicleDamageEvent event)
	{
		//all of this is anti theft code
		if(!GriefPrevention.instance.config_claims_preventTheft) return;	
		
		//input validation
		if(event.getVehicle() == null) return;
		
		//don't track in worlds where claims are not enabled
        if(!GriefPrevention.instance.claimsEnabledForWorld(event.getVehicle().getWorld())) return;
		
		//determine which player is attacking, if any
		Player attacker = null;
		Entity damageSource = event.getAttacker();
		
		//if damage source is null, don't allow the damage when the vehicle is in a land claim
		if(damageSource == null)
		{
		    Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), false, null);
	        
	        //if it's claimed
	        if(claim != null)
	        {
	            event.setCancelled(true);
	        }
	        
            return;
		}
		
		if(damageSource.getType() == EntityType.PLAYER)
		{
			attacker = (Player)damageSource;
		}
		else if(damageSource instanceof Projectile)
		{
		    Projectile arrow = (Projectile)damageSource;
		    if(arrow.getShooter() instanceof Player)
			{
				attacker = (Player)arrow.getShooter();
			}
		}
		
		//if not a player and not an explosion, always allow
        if(attacker == null && !(damageSource instanceof Explosive))
        {
            return;
        }
		
		//NOTE: vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		Claim cachedClaim = null;
		PlayerData playerData = null;
		
		if(attacker != null)
		{
			playerData = this.dataStore.getPlayerData(attacker.getUniqueId());
			cachedClaim = playerData.lastClaim;
		}
		
		Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), false, cachedClaim);
		
		//if it's claimed
		if(claim != null)
		{
			//if damaged by anything other than a player, cancel the event
			if(attacker == null)
			{
				event.setCancelled(true);
			}
			
			//otherwise the player damaging the entity must have permission
			else
			{		
				String noContainersReason = claim.allowContainers(attacker);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if(attacker.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                    event.setCancelled(true);
				}
				
				//cache claim for later
				if(playerData != null)
				{
					playerData.lastClaim = claim;
				}						
			}
		}
	}
}
