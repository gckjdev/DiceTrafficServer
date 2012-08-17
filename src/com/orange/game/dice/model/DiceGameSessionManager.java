package com.orange.game.dice.model;

import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.manager.GameSessionManager;

public class DiceGameSessionManager extends GameSessionManager {

	@Override
	public GameSession createSession(int sessionId, String name, boolean createByUser) {
		return new DiceGameSession(sessionId, name, createByUser);
	}

	@Override
	public void userQuitSession(GameSession session, String userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getGameId() {
		return DiceGameConstant.DICE_GAME_ID;
	}

	
}
