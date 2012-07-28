package com.orange.game.dice.robot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.robot.client.RobotClient;
import com.orange.game.dice.robot.manager.RobotManager;

public class RobotService {

	
	// thread-safe singleton implementation
    private static RobotService service = new RobotService();     
    private RobotService(){		
	} 	    
    public static RobotService getInstance() { 
    	return service; 
    }

    ExecutorService executor = Executors.newFixedThreadPool(RobotManager.MAX_ROBOT_USER);
    
    RobotManager robotManager = RobotManager.getInstance();
	
    public boolean isEnableRobot() {
		String robot = System.getProperty("config.robot");
		if (robot != null && !robot.isEmpty()){
			return (Integer.parseInt(robot) == 1);
		}
		return false; // default
	}	
	
    public void startNewRobot(int sessionId, String roomId) {
    	if (!isEnableRobot()){
    		ServerLog.info(sessionId, "Robot not enabled for launch");
    		return;
    	}
    	
    	int robotCount = getRobotCountPerTime();
    	
    	for (int i=0; i<robotCount; i++){
	    	RobotClient client = robotManager.allocNewClient(sessionId); 
	    	client.setRoomId(roomId);
	    	if (client == null){
	    		ServerLog.info(sessionId, "start new robot but no robot client available");
	    		return;
	    	}

	    	executor.execute(client);
    	}
    	
	}
    
	private int getRobotCountPerTime() {
		String robot = System.getProperty("config.robot_count");
		if (robot != null && !robot.isEmpty()){
			return Integer.parseInt(robot);
		}
		return 1; // default
	}
	
	public void finishRobot(final RobotClient robotClient) {		
		executor.execute(new Runnable(){
			@Override
			public void run() {
				robotManager.deallocClient(robotClient);
				robotClient.stopClient();
			}		
		});
	} 	
    
    
}
