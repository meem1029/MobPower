package com.meem1029.MobPower;

import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class DropEquation implements Equation<ItemStack> {

	private Equation<Double> howMany;
	private MaterialData mat;
	private double max,min;
	
	public DropEquation(double x1, double y1, double x2, double y2, MaterialData mat){
		howMany = new LinearEquation(x1,y1,x2,y2);
		this.mat = mat;
		this.min = 0;
		this.max = Double.MAX_VALUE;
	}
	
	public DropEquation(double x1, double y1, double x2, double y2, MaterialData mat, int min, int max){
		howMany = new LinearEquation(x1,y1,x2,y2);
		this.mat = mat;
		this.min = min;
		this.max = max;
	}
	
	
	
	public ItemStack getValue(double x){
		double num = howMany.getValue(x);
		num = Math.max(min, Math.min(max,num));
		int n = (int) Math.floor(num);
		if(Math.random() < (num - n)){
			n++;
		}
		return mat.toItemStack(n);
	}
}
