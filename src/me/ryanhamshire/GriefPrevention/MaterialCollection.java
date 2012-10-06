package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;

//ordered list of material info objects, for fast searching
public class MaterialCollection
{
	ArrayList<MaterialInfo> materials = new ArrayList<MaterialInfo>();
	
	void Add(MaterialInfo material)
	{
		int i;
		for(i = 0; i < this.materials.size() && this.materials.get(i).typeID <= material.typeID; i++);
		this.materials.add(i, material);
	}
	
	boolean Contains(MaterialInfo material)
	{
		for(int i = 0; i < this.materials.size() ; i++)
		{
			MaterialInfo thisMaterial = this.materials.get(i);
			if(material.typeID == thisMaterial.typeID && (thisMaterial.allDataValues || material.data == thisMaterial.data))
			{
				return true;
			}
			else if(thisMaterial.typeID > material.typeID)
			{
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < this.materials.size(); i++)
		{
			stringBuilder.append(this.materials.get(i).toString() + " ");
		}
		
		return stringBuilder.toString();
	}
	
	public int size()
	{
		return this.materials.size();
	}

	public void clear() 
	{
		this.materials.clear();
	}
}
