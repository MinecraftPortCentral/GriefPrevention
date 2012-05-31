package me.ryanhamshire.GriefPrevention;

import java.net.InetAddress;

public class IpBanInfo
{
	InetAddress address;
	long expirationTimestamp;
	String bannedAccountName;
	
	IpBanInfo(InetAddress address, long expirationTimestamp, String bannedAccountName)
	{
		this.address = address;
		this.expirationTimestamp = expirationTimestamp;
		this.bannedAccountName = bannedAccountName;
	}
}
