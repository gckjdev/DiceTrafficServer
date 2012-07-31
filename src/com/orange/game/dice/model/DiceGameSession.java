package com.orange.game.dice.model;

import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.dao.GameSession;

public class DiceGameSession extends GameSession {

	public DiceGameSession(int sessionId, String name) {
		super(sessionId, name);
		
		// init state
		this.currentState = DiceGameStateMachineBuilder.INIT_STATE;
	}

	
}
