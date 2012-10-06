package me.ryanhamshire.GriefPrevention;

//represents a material or collection of materials
public class MaterialInfo
{
	int typeID;
	byte data;
	boolean allDataValues;
	String description;
	
	public MaterialInfo(int typeID, byte data, String description)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = false;
		this.description = description;
	}
	
	public MaterialInfo(int typeID, String description)
	{
		this.typeID = typeID;
		this.data = 0;
		this.allDataValues = true;
		this.description = description;
	}
	
	private MaterialInfo(int typeID, byte data, boolean allDataValues, String description)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = allDataValues;
		this.description = description;
	}
	
	@Override
	public String toString()
	{
		String returnValue = String.valueOf(this.typeID) + ":" + (this.allDataValues?"*":String.valueOf(this.data));
		if(this.description != null) returnValue += ":" + this.description;
		
		return returnValue;
	}
	
	public static MaterialInfo fromString(String string)
	{
		if(string == null || string.isEmpty()) return null;
		
		String [] parts = string.split(":");
		if(parts.length < 3) return null;
		
		try
		{
			int typeID = Integer.parseInt(parts[0]);
		
			byte data;
			boolean allDataValues;
			if(parts[1].equals("*"))
			{
				allDataValues = true;
				data = 0;
			}
			else
			{
				allDataValues = false;
				data = Byte.parseByte(parts[1]);
			}
			
			return new MaterialInfo(typeID, data, allDataValues, parts[2]);
		}
		catch(NumberFormatException exception)
		{
			return null;
		}
	}
}
