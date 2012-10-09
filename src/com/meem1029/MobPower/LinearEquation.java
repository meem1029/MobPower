package com.meem1029.MobPower;

public class LinearEquation implements Equation<Double> {
	
	private double x1;
	private double y1;
	private double m;
	private double min;
	private double max;
	private static double MAX = Double.MAX_VALUE;
	private static double MIN = -MAX;
	
	public LinearEquation(double x1, double y1, double x2, double y2){
		this.x1 = x1;
		this.y1 = y1;
		this.m = (y2 - y1)/(x2-x1);
		this.min = MIN;
		this.max = MAX;
	}
	
	public LinearEquation(double x1, double y1, double x2, double y2, double min, double max){
		this.x1 = x1;
		this.y1 = y1;
		this.m = (y2 - y1)/(x2-x1);
		this.min = min;
		this.max = max;
	}
	
	public String toString(){
		return "Linear Equation with x1 of " + x1 +", y1 of " + y1 +", slope of "+m+", min of "+ min +", and max of " + max;
	}
	
	public void setMax(double max){
		this.max = max;
	}
	
	public void setMin(double min){
		this.min = min;
	}
	
	public Double getValue(double x){
		double val = Double.valueOf(y1 + m * (x-x1));
		return Math.max(min,Math.min(max, val));
	}
	
}
