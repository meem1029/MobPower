package com.meem1029.MobPower;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
	private Equation<Double> healthEquation;
	private Equation<Double> damageEquation;
	private Equation<Double> zoneEquation;
	private Map<UUID,Integer> healthMap;
	
	private final int infinity = 10000;
	
	
	public void onEnable(){
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this,this);
		log = Logger.getLogger("Minecraft");
		this.getEquations();
		healthMap = new HashMap<UUID,Integer>();
	}
	
	//Will read from a config file in the future. For now due to time, they're hard coded.
	private void getEquations(){
		healthEquation = new LinearEquation(1,0,11,3);
		damageEquation = new LinearEquation(1,0,11,5);
		zoneEquation = new LinearEquation(2000,1,0,11);
	}
	
	// Damage Changing
	@EventHandler
	public void onMobDamage(EntityDamageByEntityEvent e){
		Entity mob = e.getDamager();
		LivingEntity victim;
		try{
			victim = (LivingEntity) e.getEntity();
		}
		catch(ClassCastException x){//Victim is not a living entity. Therefore, we don't care.
			return;
		}
		if(! (isHumanCausedDamage(mob))){
			double multiplier = damageEquation.getValue(getDistance(mob.getLocation()));
			int oldDamage = e.getDamage();
			int newDamage = (int) (oldDamage * multiplier);
			//log.info("Turned " + oldDamage + " into " + newDamage);
			e.setDamage(newDamage);
		}
		if(! (victim instanceof Player)){
			UUID mobId = victim.getUniqueId();
			int health;
			try{
				health = healthMap.get(mobId);
			}
			catch(NullPointerException x){
				health = victim.getHealth();
				healthMap.put(mobId,health);
			}
			int damage = e.getDamage();
			int newHealth = health - damage;
			if(newHealth <= 0){
				healthMap.remove(mobId);//It's dead now, we don't want to track it. Note: Move this to the death event when I get one.
				e.setDamage(this.infinity);//I don't really want to look up how to kill a mob in bukkit, but this should do it.
			}
			else{
				e.setDamage(0);
				healthMap.put(mobId,newHealth);
			}
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e){
		if(getZone(e.getTo()) - getZone(e.getFrom()) != 0){
			e.getPlayer().sendMessage("You just entered zone " + getZone(e.getTo()) + ".");
		}
	}
	
	//checks if the damager is a player.
	private boolean isHumanCausedDamage(Entity e){
		if(e instanceof Player){
			return true;
		}
		if(e instanceof Arrow){
			Arrow a = (Arrow) e;
			return a.getShooter() instanceof Player;
		}
		return false;
	}
	
	// Health Changing
	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent e){
		LivingEntity mob = e.getEntity();
		int oldHealth = mob.getHealth();
		double multiplier = healthEquation.getValue(getZone(e.getLocation()));
		int newHealth = Math.max((int) (oldHealth * multiplier), 1);
		UUID mobId = mob.getUniqueId();
		healthMap.put(mobId,newHealth);
 	}
	
	
	private int getZone(Location l){
		double dist = getDistance(l);
		double zone = zoneEquation.getValue(dist);
		return (int) Math.floor(zone);
	}
	
	private double getDistance(Location l){
		int x = Math.abs(l.getBlockX());
		int z = Math.abs(l.getBlockZ());
		return Math.max(x,z);
	}
	
}
