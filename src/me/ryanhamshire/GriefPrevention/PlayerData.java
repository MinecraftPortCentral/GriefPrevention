/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.SiegeData;
import me.ryanhamshire.GriefPrevention.Visualization;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

//holds all of GriefPrevention's player-tied data
public class PlayerData 
{
	//the player's ID
	public UUID playerID;
	
	//the player's claims
	private Vector<Claim> claims = null;
	
	//how many claim blocks the player has earned via play time
	private Integer accruedClaimBlocks = null;
	
	//where this player was the last time we checked on him for earning claim blocks
	public Location lastAfkCheckLocation = null;
	
	//how many claim blocks the player has been gifted by admins, or purchased via economy integration 
	private Integer bonusClaimBlocks = null;
	
	//what "mode" the shovel is in determines what it will do when it's used
	public ShovelMode shovelMode = ShovelMode.Basic;
	
	//radius for restore nature fill mode
	int fillRadius = 0;
	
	//last place the player used the shovel, useful in creating and resizing claims, 
	//because the player must use the shovel twice in those instances
	public Location lastShovelLocation = null;	
	
	//the claim this player is currently resizing
	public Claim claimResizing = null;
	
	//the claim this player is currently subdividing
	public Claim claimSubdividing = null;
	
	//whether or not the player has a pending /trapped rescue
	public boolean pendingTrapped = false;
	
	//whether this player was recently warned about building outside land claims
	boolean warnedAboutBuildingOutsideClaims = false;
	
	//timestamp of last death, for use in preventing death message spam
	long lastDeathTimeStamp = 0;
	
	//whether the player was kicked (set and used during logout)
	boolean wasKicked = false;
	
	//spam
	private Date lastLogin = null;					//when the player last logged into the server
    public String lastMessage = "";					//the player's last chat message, or slash command complete with parameters 
	public Date lastMessageTimestamp = new Date();  //last time the player sent a chat message or used a monitored slash command
	public int spamCount = 0;						//number of consecutive "spams"
	public boolean spamWarned = false;				//whether the player recently received a warning
	
	//visualization
	public Visualization currentVisualization = null;
	
	//anti-camping pvp protection
	public boolean pvpImmune = false;
	public long lastSpawn = 0;
	
	//ignore claims mode
	public boolean ignoreClaims = false;
	
	//the last claim this player was in, that we know of
	public Claim lastClaim = null;
	
	//siege
	public SiegeData siegeData = null;
	
	//pvp
	public long lastPvpTimestamp = 0;
	public String lastPvpPlayer = "";
	
	//safety confirmation for deleting multi-subdivision claims
	public boolean warnedAboutMajorDeletion = false;

	public InetAddress ipAddress;

    //whether or not this player has received a message about unlocking death drops since his last death
	boolean receivedDropUnlockAdvertisement = false;

    //whether or not this player's dropped items (on death) are unlocked for other players to pick up
	boolean dropsAreUnlocked = false;

    //message to send to player after he respawns
	String messageOnRespawn = null;

    //player which a pet will be given to when it's right-clicked
	OfflinePlayer petGiveawayRecipient = null;
	
	//whether or not this player is "in" pvp combat
	public boolean inPvpCombat()
	{
		if(this.lastPvpTimestamp == 0) return false;
		
		long now = Calendar.getInstance().getTimeInMillis();
		
		long elapsed = now - this.lastPvpTimestamp;
		
		if(elapsed > GriefPrevention.instance.config_pvp_combatTimeoutSeconds * 1000) //X seconds
		{
			this.lastPvpTimestamp = 0;
			return false;
		}
		
		return true;
	}
	
	//the number of claim blocks a player has available for claiming land
	public int getRemainingClaimBlocks()
	{
		int remainingBlocks = this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();
		for(int i = 0; i < this.getClaims().size(); i++)
		{
			Claim claim = this.getClaims().get(i);
			remainingBlocks -= claim.getArea();
		}
		
		//add any blocks this player might have based on group membership (permissions)
		remainingBlocks += GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerID);
		
		return remainingBlocks;
	}
	
	//don't load data from secondary storage until it's needed
	public int getAccruedClaimBlocks()
	{
	    if(this.accruedClaimBlocks == null) this.loadDataFromSecondaryStorage();
        return accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(Integer accruedClaimBlocks)
    {
        this.accruedClaimBlocks = accruedClaimBlocks;
    }

    public int getBonusClaimBlocks()
    {
        if(this.bonusClaimBlocks == null) this.loadDataFromSecondaryStorage();
        return bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(Integer bonusClaimBlocks)
    {
        this.bonusClaimBlocks = bonusClaimBlocks;
    }

    public Date getLastLogin()
    {
        if(this.lastLogin == null) this.loadDataFromSecondaryStorage();
        return this.lastLogin;
    }
    
    public void setLastLogin(Date lastLogin)
    {
        this.lastLogin = lastLogin;
    }
    
    private void loadDataFromSecondaryStorage()
    {
        //reach out to secondary storage to get any data there
        PlayerData storageData = GriefPrevention.instance.dataStore.getPlayerDataFromStorage(this.playerID);
        
        //fill in any missing pieces
        if(this.lastLogin == null)
        {
            if(storageData.lastLogin != null)
            {
                this.lastLogin = storageData.lastLogin;
            }
            else
            {
                //default last login date value to 5 minutes ago to ensure a brand new player can log in
                //see login cooldown feature, PlayerEventHandler.onPlayerLogin()
                //if the player successfully logs in, this value will be overwritten with the current date and time 
                Calendar fiveMinutesBack = Calendar.getInstance();
                fiveMinutesBack.add(Calendar.MINUTE, -5);
                this.lastLogin = fiveMinutesBack.getTime();
            }
        }
        
        if(this.accruedClaimBlocks == null)
        {
            if(storageData.accruedClaimBlocks != null)
            {
                this.accruedClaimBlocks = storageData.accruedClaimBlocks;
            }
            else
            {
                this.accruedClaimBlocks = GriefPrevention.instance.config_claims_initialBlocks;
            }
        }
        
        if(this.bonusClaimBlocks == null)
        {
            if(storageData.bonusClaimBlocks != null)
            {
                this.bonusClaimBlocks = storageData.bonusClaimBlocks;
            }
            else
            {
                this.bonusClaimBlocks = 0;
            }
        }
    }
    
    public Vector<Claim> getClaims()
    {
        if(this.claims == null)
        {
            this.claims = new Vector<Claim>();
            
            //find all the claims belonging to this player and note them for future reference
            DataStore dataStore = GriefPrevention.instance.dataStore;
            for(int i = 0; i < dataStore.claims.size(); i++)
            {
                Claim claim = dataStore.claims.get(i);
                if(playerID.equals(claim.ownerID))
                {
                    this.claims.add(claim);
                }
            }
        }
        
        return claims;
    }


}