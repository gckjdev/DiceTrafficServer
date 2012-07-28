package com.orange.gameclient.draw.test.dao;

import org.jboss.netty.channel.Channel;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class ClientUser {
	String userId;
	String nickName;
	String avatarUrl;
	long sessionId;
	Boolean gender;
	Channel channel;
	int status;
	
	public int UNCONNECTED = -1;
	public int CONNECTED = 0;
	public int WAITTING = 2;
	public int PICKINGWORD = 3;
	public int PLAYING = 4;
	
	
	private static int uid;
	private static Object lockObject = new Object();
	public static String getUid() {
		return ""+uid;
	}

	public static String getUserName() {
		return "Simulator"+(uid++);
	}

	public static ClientUser getRandClinetUser(){
		synchronized (lockObject) {
		String uidString = ""+uid;
		String userNamesString = "Simulator"+(uid++);
		return new ClientUser(uidString,userNamesString,null);
		}
	}
	
	
	public ClientUser(String userId, String nickName, String avatarUrl) {
		super();
		this.userId = userId;
		this.nickName = nickName;
		this.avatarUrl = "http://tp1.sinaimg.cn/1808415800/180/5624934495/1";
		this.sessionId = -1;
		this.status = UNCONNECTED;
		this.gender = false;
	}
	
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getNickName() {
		return nickName;
	}
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	public String getAvatarUrl() {
		return avatarUrl;
	}
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}


	public Boolean getGender() {
		return gender;
	}


	public void setGender(Boolean gender) {
		this.gender = gender;
	}


	public Channel getChannel() {
		return channel;
	}


	public void setChannel(Channel channel) {
		this.channel = channel;
	}


	@Override
	public String toString() {
		return "ClientUser [CONNECTED=" + CONNECTED + ", PICKINGWORD="
				+ PICKINGWORD + ", PLAYING=" + PLAYING + ", UNCONNECTED="
				+ UNCONNECTED + ", WAITTING=" + WAITTING + ", avatarUrl="
				+ avatarUrl + ", gender=" + gender + ", nickName=" + nickName
				+ ", sessionId=" + sessionId + ", status=" + status
				+ ", userId=" + userId + "]";
	}
	
	
}


