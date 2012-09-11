package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;
import com.orange.network.game.protocol.message.GameMessageProtos.UserDiceNotification;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;

public class RollDiceItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId, UseItemResponse.Builder useItemResponseBuilder) {

		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		if (session.isOpen()){
			resultCode = GameResultCode.ERROR_DICE_ALREADY_OPEN;					
		}
		else{				
			// roll dice again
			PBUserDice userDice = session.rollDiceAgain(userId);
			
			// broadcast dice notification
			UserDiceNotification diceNoti = UserDiceNotification.newBuilder().addUserDice(userDice).build(); 
			NotificationUtils.broadcastUserDiceNotification(session, userId, diceNoti);
			
			useItemResponseBuilder.addAllDices(userDice.getDicesList());
		}
		
		return resultCode;
	}
}
