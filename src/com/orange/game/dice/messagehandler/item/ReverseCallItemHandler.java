
package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse.Builder;

public class ReverseCallItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId,
			Builder useItemResponseBuilder) {
		
		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		int sessionId = session.getSessionId();
		
		// Only in current play user's round shall he/she use this item!
		if ( ! session.isCurrentPlayUser(userId) ) {
			resultCode = GameResultCode.ERROR_USER_NOT_IN_SESSION;
			ServerLog.info(sessionId, userId + "wants to use [Reverse call] item, "
					+ "but he/she is not the current user!!!");
		}
		
	    // change the directions
		session.alternateSetPlayDirection();
		
		return resultCode;
	}

}
