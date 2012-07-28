package com.orange.game.dice.db;

import com.orange.common.mongodb.MongoDBClient;

public class DrawDBClient {

	public static final String DB_NAME = "game";
	
	
	MongoDBClient mongoClient = new MongoDBClient(DB_NAME);
	
	// thread-safe singleton implementation
    private static DrawDBClient client = new DrawDBClient();     
    private DrawDBClient(){		
	} 	    
    public static DrawDBClient getInstance() { 
    	return client; 
    } 
    
    public MongoDBClient getMongoClient(){
    	return mongoClient;
    }
    
}
