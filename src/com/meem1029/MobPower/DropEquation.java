package com.meem1029.MobPower;

import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class DropEquation implements Equation<ItemStack> {

	private Equation<Double> howMany;
	private MaterialData mat;
	
	public DropEquation(double x1, double y1, double x2, double y2, MaterialData mat){
		howMany = new LinearEquation(x1,y1,x2,y2);
		this.mat = mat;
	}
	
	public DropEquation(double x1, double y1, double x2, double y2, MaterialData mat, int min, int max){
		howMany = new LinearEquation(x1,y1,x2,y2,min,max);
		this.mat = mat;
	}
	
	public DropEquation(Equation<Double> eq, MaterialData mat){
		howMany = eq;
		this.mat = mat;
	}
	
	public void setMin(double min){
		howMany.setMin(min);
	}
	
	public void setMax(double max){
		howMany.setMax(max);
	}
	
	public ItemStack getValue(double x){
		double num = howMany.getValue(x);
		int n = (int) Math.floor(num);
		if(Math.random() < (num - n)){
			n++;
		}
		return mat.toItemStack(n);
	}
}
