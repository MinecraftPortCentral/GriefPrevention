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

import java.util.Calendar;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

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
		else if(event.getEntityType() == EntityType.WITHER && GriefPrevention.instance.config_claims_enabledWorlds.contains(event.getBlock().getWorld()))
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
		if( location.getWorld().getEnvironment() == Environment.NORMAL && GriefPrevention.instance.config_claims_enabledWorlds.contains(location.getWorld()) && ((isCreeper && GriefPrevention.instance.config_blockSurfaceCreeperExplosions) || (!isCreeper && GriefPrevention.instance.config_blockSurfaceOtherExplosions)))			
		{
			for(int i = 0; i < blocks.size(); i++)
			{
				Block block = blocks.get(i);
				if(GriefPrevention.instance.config_mods_explodableIds.Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
				
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
			
			//if the block is not claimed and is a log, trigger the anti-tree-top code
			else if(block.getType() == Material.LOG)
			{
				GriefPrevention.instance.handleLogBroken(block);
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
		LivingEntity entity = event.getEntity();
		
		//these rules apply only to creative worlds
		if(!GriefPrevention.instance.creativeRulesApply(entity.getLocation())) return;
		
		//chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
		SpawnReason reason = event.getSpawnReason();
		if(reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN)
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
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		LivingEntity entity = event.getEntity();
		
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
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//if involved in a siege
		if(playerData.siegeData != null)
		{
			//don't drop items as usual, they will be sent to the siege winner
			event.getDrops().clear();
			
			//end it, with the dieing player being the loser
			this.dataStore.endSiege(playerData.siegeData, null, player.getName(), true /*ended due to death*/);
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
        //FEATURE: claimed paintings are protected from breakage
		
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
        String noBuildReason = GriefPrevention.instance.allowBuild(playerRemover, event.getEntity().getLocation());
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
		//FEATURE: similar to above, placing a painting requires build permission in the claim
	
		//if the player doesn't have permission, don't allow the placement
		String noBuildReason = GriefPrevention.instance.allowBuild(event.getPlayer(), event.getEntity().getLocation());
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
			return;
        }
		
		//otherwise, apply entity-count limitations for creative worlds
		else if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
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
		//only actually interested in entities damaging entities (ignoring environmental damage)
		if(!(event instanceof EntityDamageByEntityEvent)) return;
		
		//monsters are never protected
		if(event.getEntity() instanceof Monster) return;
		
		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
		
		//determine which player is attacking, if any
		Player attacker = null;
		Arrow arrow = null;
		Entity damageSource = subEvent.getDamager();
		if(damageSource instanceof Player)
		{
			attacker = (Player)damageSource;
		}
		else if(damageSource instanceof Arrow)
		{
			arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player)
			{
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion)
		{
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player)
			{
				attacker = (Player)potion.getShooter();
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
			
			PlayerData defenderData = this.dataStore.getPlayerData(((Player)event.getEntity()).getName());
			PlayerData attackerData = this.dataStore.getPlayerData(attacker.getName());
			
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
					(attackerClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims ||
					!attackerClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims))
				{
					attackerData.lastClaim = attackerClaim;
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}
				
				Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
				if( defenderClaim != null &&
					(defenderClaim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims ||
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
		
		//FEATURE: protect claimed animals, boats, minecarts
		//NOTE: animals can be lead with wheat, vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		
		//if theft protection is enabled
		if(event instanceof EntityDamageByEntityEvent)
		{
			//if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
			if ((subEvent.getEntity() instanceof Creature && GriefPrevention.instance.config_claims_protectCreatures))
			{
				Claim cachedClaim = null;
				PlayerData playerData = null;
				if(attacker != null)
				{
					playerData = this.dataStore.getPlayerData(attacker.getName());
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
						if(event.getEntity() instanceof Villager && damageSource instanceof Monster && claim.isAdminClaim())
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
							
							GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
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
		
		//determine which player is attacking, if any
		Player attacker = null;
		Entity damageSource = event.getAttacker();
		if(damageSource instanceof Player)
		{
			attacker = (Player)damageSource;
		}
		else if(damageSource instanceof Arrow)
		{
			Arrow arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player)
			{
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion)
		{
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player)
			{
				attacker = (Player)potion.getShooter();
			}
		}
		
		//NOTE: vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		Claim cachedClaim = null;
		PlayerData playerData = null;
		if(attacker != null)
		{
			playerData = this.dataStore.getPlayerData(attacker.getName());
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
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
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
