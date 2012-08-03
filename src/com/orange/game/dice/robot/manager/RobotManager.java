package com.orange.game.dice.robot.manager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.cassandra.cli.CliParser.newColumnFamily_return;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.eclipse.jetty.util.ConcurrentHashSet;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.orange.common.log.ServerLog;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.game.constants.DBConstants;
import com.orange.game.dice.db.DrawDBClient;
import com.orange.game.dice.robot.client.RobotClient;
import com.orange.game.model.dao.User;

public class RobotManager {

	// thread-safe singleton implementation
    private static RobotManager manager = new RobotManager();     
    private RobotManager(){
    	robotUserList = findRobots();
    	for (int i=0; i<robotUserList.size(); i++)
    		freeSet.add(i);
	} 	    
    public static RobotManager getInstance() { 
    	return manager; 
    }
    
    public static final Logger log = Logger.getLogger(RobotManager.class.getName()); 

    public static final int MAX_ROBOT_USER = 8;

    public final static String ROBOT_USER_ID_PREFIX = "999999999999999999999";     
    
    ConcurrentHashSet<Integer> allocSet = new ConcurrentHashSet<Integer>();
    ConcurrentHashSet<Integer> freeSet  = new ConcurrentHashSet<Integer>();
    List<User> robotUserList = Collections.emptyList();
    Object allocLock = new Object();
    
    
    
    public static boolean isRobotUser(String userId){
    	if (userId == null)
    		return false;
    	
    	return userId.contains(ROBOT_USER_ID_PREFIX);
    }
    
    public int allocIndex(){
    		if (freeSet.isEmpty() ||
    			freeSet.iterator() == null)
    			return -1;
    		
    		Random random = new Random();
    		random.setSeed(System.currentTimeMillis());
    		int randomCount = random.nextInt(freeSet.size());
    		Iterator<Integer> iter = freeSet.iterator();
    		
    		
    		int index = 0;
    		while (iter != null && iter.hasNext() && index < randomCount){
    			index++;
    			iter.next();
    		}
    		
    		if (iter != null && iter.hasNext()){
    			index = iter.next().intValue();
    		}
    		
    		if (index == -1)
    			return -1;
    		
    		allocSet.add(index);
    		freeSet.remove(index);

    		ServerLog.info(0, "alloc robot, alloc index="+index + ", active robot count = "+allocSet.size());
    		return index;
    }
    
    public void deallocIndex(int index){
    	if (!isValidIndex(index) && index < robotUserList.size()){
    		return;
    	}
    	
		freeSet.add(index);
		allocSet.remove(index);
    	
    	ServerLog.info(0, "dealloc robot, index="+index + ", active robot count = "+allocSet.size());
    }
    
    public RobotClient allocNewClient(int sessionId) {        	
    	
    	int index = allocIndex();    	
    	if (!isValidIndex(index)){
    		return null;
    	}
    	
    	String nickName = "";
    	String userId = ROBOT_USER_ID_PREFIX+"000";
    	String avatar = "";
    	boolean gender = false;
    	String location = "";
    	
    	User robotUser = findRobotByIndex(index);
    	if (robotUser != null) {
    		nickName = robotUser.getNickName();
        	userId = robotUser.getUserId();
        	avatar = robotUser.getAvatar();
        	gender = (robotUser.getGender().equals("m"));
        	location = robotUser.getLocation();
		}
    	   	
    	RobotClient client = new RobotClient(userId, nickName, avatar, gender, location, sessionId, index);    	
		return client;
	}
	
    private boolean isValidIndex(int index) {
		return (index >= 0);
	}
	
	public void deallocClient(RobotClient robotClient) {
		if (robotClient == null)
			return;
		
		this.deallocIndex(robotClient.getClientIndex());				
	} 
	
	public User findRobotByIndex (int index) {
		if (robotUserList != null && !robotUserList.isEmpty() && index < robotUserList.size()) {
			return robotUserList.get(index);
		}
		return null;
	}
	
	public List<User> findRobots () {
		MongoDBClient mongoClient = DrawDBClient.getInstance().getMongoClient();
		if (mongoClient == null)
            return Collections.emptyList();
		BasicDBObject query = new BasicDBObject(DBConstants.F_ISROBOT, 1);
		BasicDBObject orderBy = new BasicDBObject(DBConstants.F_USERID, 1);
		List<User> list = new ArrayList<User>();
        DBCursor cursor = mongoClient.find(DBConstants.T_USER, query, orderBy, 0, 0);
        if (cursor != null) {
			while (cursor.hasNext()) {
				DBObject dbObject = (DBObject) cursor.next();
				list.add(new User(dbObject));
			}
			cursor.close();
		}
        return list;
	}
}
