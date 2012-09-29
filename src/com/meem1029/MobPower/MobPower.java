package com.meem1029.MobPower;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
		healthEquation = new LinearEquation(2000,1,0,26);
		damageEquation = new LinearEquation(2000,0,0,5);
	}
	
	// Damage Changing
	@EventHandler
	public void onMobDamage(EntityDamageByEntityEvent e){
		Entity mob = e.getDamager();
		Entity victim = e.getEntity();
		int oldDamage = e.getDamage();
		if((checkDamager(mob))){
			double multiplier = damageEquation.getValue(getDistance(mob.getLocation()));
			int newDamage = (int) (oldDamage * multiplier);
			//log.info("Turned " + oldDamage + " into " + newDamage);
			oldDamage = newDamage;//To allow the next part to work off of the same damage value.
			e.setDamage(newDamage);
		}
		if(! (victim instanceof Player)){
			double multiplier = healthEquation.getValue(getDistance(victim.getLocation()));
			int newDamage = Math.max((int) (oldDamage / multiplier),1);
			//log.info("Turned " + oldDamage + " into " + newDamage);
			e.setDamage(newDamage);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e){
		if(getLevel(e.getTo()) - getLevel(e.getFrom()) != 0){
			e.getPlayer().sendMessage("You just entered zone " + getLevel(e.getTo()) + ".");
		}
	}
	private int getLevel(Location l){
		return (int) Math.floor((2000 - getDistance(l))/200);
	}
	
	//checks if the damager is a player. Returns false if it is and true if it's not.
	private boolean checkDamager(Entity e){
		if(e instanceof Player){
			return false;
		}
		if(e instanceof Arrow){
			Arrow a = (Arrow) e;
			if(a.getShooter() instanceof Player){
				return false;
			}
		}
		return true;
	}
	
	
	/*
	 * I want to keep this in case I figure out a way around bukkits 20 max health...
	// Health Changing
	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent e){
		LivingEntity mob = e.getEntity();
		int oldHealth = mob.getHealth();
		double multiplier = healthEquation.getValue(getDistance(e.getLocation()));
		int newHealth = Math.max((int) (oldHealth * multiplier), 1);
		mob.setHealth(newHealth);
 	}
 	*/
	
	private double getDistance(Location l){
		int x = Math.abs(l.getBlockX());
		int z = Math.abs(l.getBlockZ());
		return Math.max(x,z);
	}
	
}
