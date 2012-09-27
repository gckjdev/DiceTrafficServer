
package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse.Builder;


public class DecTimeItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId,
			Builder useItemResponseBuilder) {
		
		GameResultCode  gameResultCode = GameResultCode.SUCCESS;
		
		int extendTime = message.getUseItemRequest().getExtendTime();
		if ( extendTime < 0 || extendTime > DiceGameStateMachineBuilder.WAIT_CLAIM_TIMEOUT) {
			gameResultCode = GameResultCode.ERROR_EXCESS_TIME_LIMIT;
		}

		int newTime = DiceGameStateMachineBuilder.WAIT_CLAIM_TIMEOUT + extendTime;
		session.setNewInternal(newTime);

		// To indicate the state that next player's timer get decreased.
		session.setDecreaseTimeForNextPlayUser(true);
		
		return gameResultCode;
	}

}
