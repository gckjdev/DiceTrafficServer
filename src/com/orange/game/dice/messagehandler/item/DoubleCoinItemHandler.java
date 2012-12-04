package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.game.dice.model.DiceGameConstant;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;

public class DoubleCoinItemHandler implements ItemHandleInterface {  
	
	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId, UseItemResponse.Builder useItemResponseBuilder) {
		
		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		if (session.isOpen()){
			resultCode = GameResultCode.ERROR_DICE_ALREADY_OPEN;											
		}
		else{
			int multiple = 2;
			resultCode = DiceGameAction.openDiceAndBroadcast(session, userId, DiceGameConstant.DICE_OPEN_TYPE_CUT, multiple);
		}
							
		if (resultCode == GameResultCode.SUCCESS){
			// fire event
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_OPEN_DICE, session.getSessionId(), userId);				
		}
		
		return resultCode;
	}

}
