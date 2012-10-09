package com.meem1029.MobPower;

import java.util.ArrayList;
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
	private boolean debug = false;
	
	//Config variables.
	private String equationName = "Equation";
	private String zoneName = "Zones";
	private String healthName = "Health";
	private String damageName = "Damage";
	private String dropName = "Drops";
	private String dropNamesName = "Names";
	private String dropMobsName = "Mobs";
	private String minName = "Min";
	private String maxName = "Max";
	private String dropItemName = "Item";
	
	public void onEnable(){
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this,this);
		
		log = Logger.getLogger("Minecraft");
		
		this.config = getConfig();
		this.getEquations();
		healthMap = new HashMap<UUID,Integer>();
		damageMap = new HashMap<UUID,Double>();
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
		//log.info(zoneEquation.toString());
		//log.info(healthEquation.toString());
		//log.info(damageEquation.toString());
		//log.info(dropMap.toString());
	}
	
	//Gets the Equation at the location specified in the config by the given string.
	private Equation<Double> getEquation(String base){
		String configName = base + "." + equationName;
		//log.info(configName);
		Equation<Double> res = parseEquation(splitToDoubles(config.get(configName)));
		if(config.isSet(base + "." + minName)){
			res.setMin(config.getDouble(base + "." + minName));
		}
		if(config.isSet(base + "." + maxName)){
			res.setMax(config.getDouble(base + "." + maxName));
		}
		return res;
	}
	
	private List<Double> splitToDoubles(Object o){
		if(!(o instanceof String)){
			log.info("GRRRRRRRR");
			log.info((String) o);
			log.info(o.getClass().toString());
		}
		String s = (String) o;
		String[] arr = s.split("\\s+");
		List<Double> res = new ArrayList<Double>();
		for(int i = 0;i<arr.length;i++){
			res.add(Double.valueOf(arr[i]));
		}
		return res;
	}
	
	private Equation<Double> parseEquation(List<Double> vals){
		Double[] arr = {};
		arr = vals.toArray(arr);
		int size = arr.length;
		//log.info("Parsing new equation of size "+size);
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
		List<String> names = config.getStringList(dropName + "." + dropNamesName);
		for(String s:names){
			if(debug){
				log.info(s);
			}
			List<String> mobs = config.getStringList(dropName + "." + s + "." + dropMobsName);
			DropEquation equation = getDropEquation(dropName + "." + s);
			for(String m: mobs){
				EntityType mobType = EntityType.fromName(m);
				if(m.equalsIgnoreCase("All")){
					mobType = EntityType.UNKNOWN;
				}
				dropMap.get(mobType).add(equation);
			}
		} 
	}
	
	private DropEquation getDropEquation(String path){
		Equation<Double> eq = getEquation(path);
		Integer[] material = {};
		material = config.getIntegerList(path + "." + dropItemName).toArray(material);
		if(material.length == 1){
			return new DropEquation(eq,new MaterialData(material[0]));
		}
		else if(material.length == 0){
			return new DropEquation(eq,new MaterialData(config.getInt(path + "." + dropItemName)));
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
	
	//For manually adding items for when I want to kill the config.
	private void addItem(EntityType et, Material mat, double x1, double y1, double x2, double y2){
		dropMap.get(et).add(new DropEquation(x1,y1,x2,y2,new MaterialData(mat)));
	}
	private void addItem(EntityType et, Material mat){
		addItem(et,mat,1,0,11,2);
	}
	
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
		if((getZone(e.getTo()) - getZone(e.getFrom()) != 0)&& (getZone(e.getTo()) % 10 == 0)){
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
		if(debug){
			log.info(" Mob is in zone "+ getZone(mob.getLocation()));
			log.info("Giving "+ mobId.toString() + " health going from " + oldHealth + " to " + newHealth + " with a multiplier of " + multiplier + ".");
		}
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
			if(debug){
				log.info("giving item");
			}
			items.add(eq.getValue(location));
		}
		for(DropEquation eq: dropMap.get(mobType)){
			if(debug){
				log.info("adding item");
			}
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