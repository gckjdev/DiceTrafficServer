package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;

public class DefaultItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId, UseItemResponse.Builder useItemResponseBuilder) {
		return GameResultCode.SUCCESS;
	}

}
