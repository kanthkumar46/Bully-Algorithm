package com.rit.bully;

public enum Messages {
	OK(1), 
	Election(2), 
	Leader(3);
	
	int message;
	private Messages(int message) {
		this.message = message;
	}
}

