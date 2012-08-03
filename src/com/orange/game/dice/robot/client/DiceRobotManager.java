package com.orange.game.dice.robot.client;

import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.game.traffic.robot.client.AbstractRobotManager;

public class DiceRobotManager extends AbstractRobotManager {

	@Override
	public AbstractRobotClient createRobotClient(String userId,
			String nickName, String avatar, boolean gender, 
			String location, int sessionId, int index) {
		return new DiceRobotClient(userId, nickName, avatar, gender, location, sessionId, index);
	}

}
