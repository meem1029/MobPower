package com.meem1029.MobPower;

public interface Equation<T> {
	public T getValue(double x);
	public void setMin(double min);
	public void setMax(double max);
}
