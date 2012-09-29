package com.meem1029.MobPower;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MobPower extends JavaPlugin{
	
	private Equation healthEquation;
	private Equation damageEquation;
	
	
	
	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent e){
		LivingEntity mob = e.getEntity();
		int oldHealth = mob.getHealth();
		double multiplier = healthEquation.getValue(getDistance(e.getLocation()));
		int newHealth = Math.max((int) (oldHealth * multiplier), 1);
		mob.setHealth(newHealth);
 	}
	
	private double getDistance(Location l){
		int x = l.getBlockX();
		int z = l.getBlockZ();
		return Math.max(x,z);
	}
	
}
