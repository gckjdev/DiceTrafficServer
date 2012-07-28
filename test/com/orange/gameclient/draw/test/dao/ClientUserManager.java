package com.orange.gameclient.draw.test.dao;

import java.util.HashMap;

public class ClientUserManager {

	private static HashMap<String, ClientUser> clientMap = 
		new HashMap<String, ClientUser>(); 
	
	public static void addUser(ClientUser user) {
		if (user == null || user.getUserId() == null) {
			return;
		}
		clientMap.put(user.getUserId(), user);
	}
	
	public static ClientUser getClientUserByUserId(String userId) {
		if (userId == null) {
			return null;
		}
		return clientMap.get(userId);
	}
}
