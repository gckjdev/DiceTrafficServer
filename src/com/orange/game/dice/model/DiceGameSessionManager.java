package com.orange.game.dice.model;

import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.model.manager.GameSessionManager;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;

public class DiceGameSessionManager extends GameSessionManager {

	
	@Override
	public GameSession createSession(int sessionId, String name,
			String password, boolean createByUser, String createBy,
			int ruleType, int maxPlayerCount, int testEnable) {
		return new DiceGameSession(sessionId, name, password, createByUser, createBy, ruleType,maxPlayerCount, testEnable);
	}



	@Override
	public String getGameId() {
		return DiceGameConstant.GAME_ID_DICE;
	}

	// from GameConstantProtos
	// RULE_NORMAL_VALUE = 0;
	// RULE_HIGH_VALUE = 1;
	// RULE_SUPER_HIGH_VALUE = 2;
	static int ruleType = loadRuleTypeFromConfig();		
	public static int loadRuleTypeFromConfig() {
		String ruleType = System.getProperty("ruletype");
		if (ruleType != null && !ruleType.isEmpty()){
			return Integer.parseInt(ruleType);
		}
		return DiceGameRuleType.RULE_NORMAL_VALUE; // default
	}

	@Override
	public int getRuleType() {
		return ruleType;
	}
	
	@Override
	// On: 1, Off:0[default]
	public int getTestEnable() {
			String testEnable = System.getProperty("test_enable");
			if (testEnable != null && !testEnable.isEmpty()){
				return Integer.parseInt(testEnable);
			}
			return 0;
	}

	@Override
	public boolean takeOverWhenUserQuit(GameSession session, GameUser quitUser, int sessionUserCount) {
		return (quitUser.isPlaying() == true)		// if user is not playing, don't need to take over 
				&& (sessionUserCount > 1);			// if only one user, dont' need to take over
	}

	@Override
	public void updateQuitUserInfo(GameSession session, GameUser quitUser) {		
	}

	
	@Override
	public int getMaxPlayerCount() {
		
		return readMaxPlayerCount(DiceGameConstant.MAX_PLAYER_PER_SESSION);
	}


	
}
