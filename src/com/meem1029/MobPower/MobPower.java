package com.meem1029.MobPower;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MobPower extends JavaPlugin implements Listener{

	private Logger log;
	private FileConfiguration config;
	
	private Equation<Double> healthEquation;
	private Equation<Double> damageEquation;
	private Equation<Double> zoneEquation;
	private Map<UUID,Integer> healthMap;
	private Map<UUID,Double> damageMap;
	private Map<EntityType,Set<DropEquation>> dropMap;//Note: The EntityType UNKNOWN will be applied to all drops.
	
	private boolean deathBroadcastEnabled = true;
	private boolean replaceDrops = false;
	private final int infinity = 10000;
	
	//Config variables.
	private String equationName = "Equation";
	private String zoneName = "Zone";
	private String healthName = "Health";
	private String damageName = "Damage";
	private String dropName = "Drops";
	private String minName = "Min";
	private String maxName = "Max";
	private String itemName = "Item";
	
	public void onEnable(){
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this,this);
		log = Logger.getLogger("Minecraft");
		this.getEquations();
		healthMap = new HashMap<UUID,Integer>();
	}
	
	//Will read from a config file in the future. For now due to time, they're hard coded.4
	private void getEquations(){
		try{
			zoneEquation = getEquation(zoneName);
		}
		catch(IllegalArgumentException x){
			log.info("Not enough arguments for the zone function!");
		}
		try{
			healthEquation = getEquation(healthName);
		}
		catch(IllegalArgumentException x){
			log.info("Not enough arguments for the health function!");
		}
		try{
			damageEquation = getEquation(damageName);
		}
		catch(IllegalArgumentException x){
			log.info("Not enough arguments for the damage function!");
		}
		setDropMap();
	}
	
	//Gets the Equation at the location specified in the config by the given string.
	private Equation<Double> getEquation(String base){
		Equation<Double> res = parseEquation(config.getDoubleList(base + "." + equationName));
		double min = config.getDouble(base + "." + minName);
		double max = config.getDouble(base + "." + maxName);
		if(((Double) min) !=  null){
			res.setMin(min);
		}
		if(((Double) max) != null){
			res.setMax(max);
		}
		return res;
	}
	
	private Equation<Double> parseEquation(List<Double> vals){
		Double[] arr = {};
		arr = vals.toArray(arr);
		int size = arr.length;
		if(size < 4){
			throw new IllegalArgumentException("Not enough values in list.");
		}
		if(size >= 6){
			return new LinearEquation(arr[0],arr[1],arr[2],arr[3],arr[4],arr[5]);
		}
		else{
			return new LinearEquation(arr[0],arr[1],arr[2],arr[3]);
		}
	}
	
	private void setDropMap(){
		dropMap = new HashMap<EntityType,Set<DropEquation>>();
		//Initialize all possible elements of the dropMap. This takes up a bit of space, but I don't much care because it's not much and made up for by the ease it adds.
		for(EntityType et:EntityType.values()){
			dropMap.put(et,new HashSet<DropEquation>());
		}
		ConfigurationSection dropConfig = config.getConfigurationSection(dropName);
		for(String s:dropConfig.getKeys(false)){
			String mob = getMobName(s);
			EntityType mobType = EntityType.fromName(mob);
			if(mob.equalsIgnoreCase("All")){
				mobType = EntityType.UNKNOWN;
			}
			for(String path:config.getConfigurationSection(s).getKeys(false)){
				getDropEquation(path,mobType);
			}
		} 
	}
	
	private DropEquation getDropEquation(String path, EntityType mob){
		Equation<Double> eq = getEquation(path);
		Integer[] material = {};
		material = config.getIntegerList(path + "." + itemName).toArray(material);
		if(material.length == 1){
			return new DropEquation(eq,new MaterialData(material[0]));
		}
		else{
			return new DropEquation(eq,new MaterialData(material[0],material[1].byteValue()));
		}
	}
	
	//Takes a yaml string identifier and extracts the last from it.
	private String getMobName(String s){
		String[] parts = s.split(".");
		return parts[parts.length - 1];
	}
	
	/*
	private void addItem(EntityType et, Material mat, double x1, double y1, double x2, double y2){
		dropMap.get(et).add(new DropEquation(x1,y1,x2,y2,new MaterialData(mat)));
	}
	private void addItem(EntityType et, Material mat){
		addItem(et,mat,1,0,11,2);
	}*/
	
	// Damage/Health Changing
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
			if(mob instanceof Arrow){
				mob = ((Arrow)mob).getShooter();
			}
			UUID mobId = mob.getUniqueId();
			double multiplier;
			try{
				multiplier = damageMap.get(mobId);
			}
			catch(NullPointerException x){
				multiplier = damageEquation.getValue(getDistance(mob.getLocation()));
				damageMap.put(mobId, multiplier);
			}
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
				setHealth(victim);
				health = healthMap.get(mobId);
			}
			int damage = e.getDamage();
			int newHealth = health - damage;
			if(newHealth <= 0){
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
	
	
	private void setHealth(LivingEntity mob){
		int oldHealth = mob.getHealth();
		double multiplier = healthEquation.getValue(getZone(mob.getLocation()));
		int newHealth = Math.max((int) (oldHealth * multiplier), 1);
		UUID mobId = mob.getUniqueId();
		healthMap.put(mobId,newHealth);
	}
	
	//Drop Handling
	@EventHandler
	public void onEntityDeath(EntityDeathEvent e){
		LivingEntity victim = e.getEntity();
		EntityType mobType = victim.getType();
		UUID mobId = victim.getUniqueId();
		Location sceneOfTheCrime = victim.getLocation();
		healthMap.remove(mobId);
		damageMap.remove(mobId);
		
		
		if(deathBroadcastEnabled){
			String deathMessage = deathMessage(victim);
			if(deathMessage.length()>0){
				getServer().broadcastMessage(deathMessage);
			}
		}
		Player killer = victim.getKiller();
		if(killer == null){//Mob was not killed by a player. Likely was by another mob,fall damage, fire, etc.
			return;//Thus we don't want to do anything.
		}
		List<ItemStack> items = e.getDrops();
		double zone = getZone(sceneOfTheCrime);
		items = adjustDrops(mobType, zone, items);
	}
	
	private List<ItemStack> adjustDrops(EntityType mobType, double location, List<ItemStack> items){
		//Note: The drops of EntityType UNKNOWN will be applied to all entities.
		if(replaceDrops){
			items.clear();
		}
		for(DropEquation eq: dropMap.get(EntityType.UNKNOWN)){
			log.info("giving item");
			items.add(eq.getValue(location));
		}
		for(DropEquation eq: dropMap.get(mobType)){
			log.info("adding item");
			items.add(eq.getValue(location));
		}
		return items;
	}
	
	public String deathMessage(LivingEntity noLongerLivingEntity){
		Location corpseLoc = noLongerLivingEntity.getLocation();        
        
        EntityType corpseType = noLongerLivingEntity.getType();
        
        String killer = "";
        String mob= "";
        String message = "";

        int zone = getZone(corpseLoc);
        
        if(noLongerLivingEntity.getKiller()!=null){
        
            killer = noLongerLivingEntity.getKiller().getName();
            
            mob = corpseType.getName().toLowerCase();    
            message = "A level "+zone+" "+mob+" has been killed by " + killer;
        
        }
            
        return message;
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