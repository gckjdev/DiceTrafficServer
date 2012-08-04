package com.orange.game.dice.model;

import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.manager.GameSessionManager;

public class DiceGameSessionManager extends GameSessionManager {

	@Override
	public GameSession createSession(int sessionId, String name) {
		return new DiceGameSession(sessionId, name);
	}

	@Override
	public void userQuitSession(GameSession session, String userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getGameId() {
		// TODO Auto-generated method stub
		return "Dice";
	}

	
}
