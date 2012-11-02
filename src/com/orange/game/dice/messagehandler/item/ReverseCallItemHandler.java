
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
		// Must set the directionChanged flag to true.
		// Think about this:
		// If B changes directon and then skip, it's A's
		// turn to play, but since the callUserId is A,
		// then A pass without calling, this is a bug. So we set
		//  the flag and check this flag at the same time check
		// the callUserId in DiceGameSession.call().
		session.setDirectionChanged(true);
		
		return resultCode;
	}

}
