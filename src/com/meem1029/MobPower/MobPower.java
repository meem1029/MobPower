package com.meem1029.MobPower;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MobPower extends JavaPlugin implements Listener{

	private Logger log;
	private Equation healthEquation;
	private Equation damageEquation;
	
	
	public void onEnable(){
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this,this);
		log = Logger.getLogger("Minecraft");
		this.getEquations();
	}
	
	//Will read from a config file in the future. For now due to time, they're hard coded.
	private void getEquations(){
		healthEquation = new LinearEquation(2000,1,1800,3.5);
		damageEquation = new LinearEquation(2000,0,1600,1);
	}
	
	// Damage Changing
	@EventHandler
	public void onMobDamage(EntityDamageByEntityEvent e){
		Entity mob = e.getDamager();
		Entity victim = e.getEntity();
		if(mob instanceof Player){
			return;
		}
		int oldDamage = e.getDamage();
		double multiplier = damageEquation.getValue(getDistance(mob.getLocation()));
		int newDamage = (int) (oldDamage * multiplier);
		e.setDamage(newDamage);
	}
	
	// Health Changing
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
