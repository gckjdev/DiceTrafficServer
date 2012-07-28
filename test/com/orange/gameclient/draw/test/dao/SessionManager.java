package com.orange.gameclient.draw.test.dao;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.orange.gameclient.draw.test.GameClientHandler;


public class SessionManager {
	private static HashMap<Long, Integer>sessionMap 
	= new HashMap<Long, Integer>();
	
	private static final Logger logger = Logger.getLogger(SessionManager.class
			.getName());

	private static final Integer MAX_COUNT = 6; 
	private static int count = 0;
	
	
	public static  void increaseCount(long sessionID)
	{
		synchronized (sessionMap) {	
			Integer countInteger = sessionMap.get(Long.valueOf(sessionID));
			if (countInteger != null) {
				countInteger = countInteger + 1;
			}else{
				countInteger = 1;
			}
			if (countInteger > MAX_COUNT){
				logger.error("session " + sessionID + " session count " + countInteger + " bigger than max "+ MAX_COUNT);
			}
			sessionMap.put(Long.valueOf(sessionID), countInteger);
			logger.info("user count = " + (count ++) + " "+ "session count = "+sessionMap.keySet().size());

		}
	}
	
	
	public static  void decreaseCount(long sessionID)
	{
		synchronized (sessionMap) {	
			Integer countInteger = sessionMap.get(Long.valueOf(sessionID));
			countInteger --;		
			sessionMap.put(Long.valueOf(sessionID), countInteger);
//			logger.info("user count = " + (count --) + "�� "+ "session count = "+sessionMap.keySet().size());

		}
	}
	
	public static int count(long sessionID)
	{
		synchronized (sessionMap) {
			Integer countInteger = sessionMap.get(Long.valueOf(sessionID));
			if (countInteger == null) {
				return 0;
			}
			return countInteger;			
		}
	}
	
	public static String getString() {
		return sessionMap.toString();
	}
}
