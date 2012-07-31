package com.orange.game.dice.server;

import com.orange.common.statemachine.StateMachine;
import com.orange.game.dice.model.DiceGameSessionManager;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.traffic.server.GameServer;

public class DiceGameServer {
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// init data
		StateMachine diceStateMachine = DiceGameStateMachineBuilder.getInstance().buildStateMachine();
		DiceGameSessionManager sessionManager = new DiceGameSessionManager();
		
		// create server
		GameServer server = new GameServer(new DiceGameServerHandler(), diceStateMachine, sessionManager);
		
		// start server
		server.start();
	}

}
