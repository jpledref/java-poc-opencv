package com.enums;

public enum Extension implements IEnums{
	
	WIN(".dll"),
	NIX(".so");


	private String label;
	
	private Extension(String lib){
		this.label = lib;
	}
	
	public String getLabel() {
		  return label;
	}
	
}
