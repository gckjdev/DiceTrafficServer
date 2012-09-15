package com.orange.game.dice.model;

import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.manager.GameSessionManager;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;

public class DiceGameSessionManager extends GameSessionManager {

	@Override
	public GameSession createSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType) {
		return new DiceGameSession(sessionId, name, password, createByUser, createBy, ruleType);
	}

	@Override
	public void userQuitSession(GameSession session, String userId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getGameId() {
		return DiceGameConstant.DICE_GAME_ID;
	}

	
	@Override
	// from GameConstantProtos
	// RULE_NORMAL_VALUE = 0;
	// RULE_HIGH_VALUE = 1;
	// RULE_SUPER_HIGH_VALUE = 2;
	public int getRuleType() {
		String ruleType = System.getProperty("ruletype");
		if (ruleType != null && !ruleType.isEmpty()){
			return Integer.parseInt(ruleType);
		}
		return DiceGameRuleType.RULE_NORMAL_VALUE; // default
	}


	
}
