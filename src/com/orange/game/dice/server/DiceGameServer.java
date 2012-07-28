package com.orange.game.dice.server;

import com.orange.common.statemachine.StateMachine;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.traffic.server.GameServer;

public class DiceGameServer {
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StateMachine diceStateMachine = DiceGameStateMachineBuilder.getInstance().buildStateMachine();
		GameServer server = new GameServer(new DiceGameServerHandler(), diceStateMachine);
		server.start();
	}

}
