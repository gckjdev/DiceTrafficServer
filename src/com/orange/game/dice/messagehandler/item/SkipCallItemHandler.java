package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse.Builder;

public class SkipCallItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId,
			Builder useItemResponseBuilder) {
		
		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		int sessionId = session.getSessionId();

		// Only in current play user's round shall he/she use this item!
		if ( ! session.isCurrentPlayUser(userId) ) {
			resultCode = GameResultCode.ERROR_USER_NOT_IN_SESSION;
			ServerLog.info(sessionId, userId + "wants to use [skip call] item, "
					+ "but he/she is not the current user!!!");
			return resultCode;
		}
		
		// set the callUserId to self to pretend I have did the call.
		// Or in DiceGameSession.call(), the check of callDiceUserId's
		// eqaulity with userId will failed.
		session.setCallDiceUserId(userId);
		GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_USER_SKIP, sessionId, userId);
		// the currentDice, currentDiceNum, isWilds remain the same, needn't change.
		
		return resultCode;
	}

}
