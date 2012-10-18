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
			resultCode = GameResultCode.ERROR_USER_NOT_CURRENT_PLAY_USER;
			ServerLog.info(sessionId, userId + "wants to use [skip call] item, "
					+ "but he/she is not the current user!!!");
			return resultCode;
		}
		
		// Bugfix: We should not set current user as the calluser
		//         since he/she uses [skip]. Otherwise if the next
		//         player challenges, current user would be blamed!
		//         However, if there are only two players, [skip]
		//         MUST be prohbited, or the calluser will remains
		//         and the game could not move on.
//		session.setCallDiceUserId(userId);
		GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_USER_SKIP, sessionId, userId);
		// the currentDice, currentDiceNum, isWilds remain the same, needn't change.
		
		return resultCode;
	}

}
