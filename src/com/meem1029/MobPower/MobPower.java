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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MobPower extends JavaPlugin implements Listener{

	private Logger log;
	private Equation<Double> healthEquation;
	private Equation<Double> damageEquation;
	private Equation<Double> zoneEquation;
	private Map<UUID,Integer> healthMap;
	private Map<UUID,Double> damageMap;
	private Map<EntityType,Set<DropEquation>> dropMap;//Note: The EntityType UNKNOWN will be applied to all drops.
	
	private boolean deathBroadcastEnabled = true;
	private boolean replaceDrops = false;
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
		setDropMap();
	}
	
	private void setDropMap(){
		dropMap = new HashMap<EntityType,Set<DropEquation>>();
		//Initialize all possible elements of the dropMap. This takes up a bit of space, but I don't much care because it's not much and made up for by the ease it adds.
		for(EntityType et:EntityType.values()){
			dropMap.put(et,new HashSet<DropEquation>());
		}
		//dropMap.get(EntityType.UNKNOWN).add(new DropEquation(1,0,11,2.5,new MaterialData(Material.EXP_BOTTLE)));
		//dropMap.get(EntityType.SKELETON).add(new DropEquation(1,0,11,2.5,new MaterialData(Material.GREEN_RECORD)));
		addItem(EntityType.UNKNOWN,Material.EXP_BOTTLE);
		addItem(EntityType.SHEEP,Material.RAW_BEEF,1,.5,11,1);//Just an example of what you can do. Drops almost certainly at the center, 50% at edges. 
	}
	
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
        
            killer =noLongerLivingEntity.getKiller().getName();
            
            /* You can't switch on a String... Perhaps this has changed in Java 7, but backwards compatibility is good. /me glares at Sat.
             * Anyway, for this purpose it's useless since we can just convert the string to all lower case.
             switch(corpseType.getName()){
             
                    
                case "Creeper": mob = "creeper";
                break;
                case "Skeleton": mob = "skeleton";
                break;
                case "Spider": mob = "spider";
                break;
                case "Zombie": mob = "zombie";
                break;
                case "Enderman": mob = "enderman";
                break;
                case "Cavespider": mob = "cavespider";
                break;
                case "Pig": mob = "pig";
                break;
                case "Sheep": mob = "sheep";
                break;
                case "Cow": mob = "cow";
                break;
                case "Chicken": mob = "chicken";
                break;
                case "Wolf": mob = "wolf";
                break;
            }
            */
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
