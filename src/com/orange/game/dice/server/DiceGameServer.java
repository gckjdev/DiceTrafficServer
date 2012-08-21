package com.orange.game.dice.server;

import java.net.ServerSocket;

import com.orange.common.statemachine.StateMachine;
import com.orange.game.dice.model.DiceGameSessionManager;
import com.orange.game.dice.robot.client.DiceRobotManager;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameServer;
import com.orange.game.traffic.server.ServerMonitor;

public class DiceGameServer {
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		RobotService.getInstance().initRobotManager(new DiceRobotManager());
		
		// init data
		StateMachine diceStateMachine = DiceGameStateMachineBuilder.getInstance().buildStateMachine();
		DiceGameSessionManager sessionManager = new DiceGameSessionManager();
		
		// create server
		GameServer server = new GameServer(new DiceGameServerHandler(), diceStateMachine, sessionManager);
		
		// start server
		server.start();
		
		// This code is to initiate the listener.
		ServerSocket srv = null;
		try {
		    srv = new ServerSocket(2222);
		    ServerMonitor monitor = new ServerMonitor(srv);
		    monitor.start();
		} catch (Exception e) {
		    System.out.println(e.getMessage());
		}
	}

}
