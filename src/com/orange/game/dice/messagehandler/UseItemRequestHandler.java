package com.orange.game.dice.messagehandler;

import javax.jws.soap.SOAPBinding.Use;
import javax.management.Notification;

import org.eclipse.jetty.server.UserIdentity;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameConstant;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.HandlerUtils;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;
import com.orange.network.game.protocol.message.GameMessageProtos.UserDiceNotification;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;

public class UseItemRequestHandler extends AbstractMessageHandler {

	public UseItemRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession gameSession) {

		DiceGameSession session = (DiceGameSession)gameSession;
		
		if (session == null){
			return;
		}
		
		String userId = message.getUserId();
		if (userId == null){
			return;
		}
		
		UseItemRequest request = message.getUseItemRequest();
		if (request == null){
			return;
		}
		
		int itemId = request.getItemId();
		ServerLog.info(session.getSessionId(), "<UseItem> itemId="+itemId);
		
		GameResultCode resultCode = GameResultCode.SUCCESS;
		switch (itemId){
		case DiceGameConstant.DICE_ITEM_ROLL_DICE_AGAIN:
			{
				if (session.isOpen()){
					resultCode = GameResultCode.ERROR_DICE_ALREADY_OPEN;
					
					// send success response with new rolled dice
					UseItemResponse useItemResponse = UseItemResponse.newBuilder()
						.setItemId(itemId)
						.build();
					
					GameMessage response = GameMessage.newBuilder()
						.setCommand(GameCommandType.USE_ITEM_RESPONSE)
						.setMessageId(message.getMessageId())
						.setResultCode(resultCode)
						.setUseItemResponse(useItemResponse)
						.setUserId(userId)
						.build();
					sendResponse(response);	
					
				}
				else{				
					// roll dice again
					PBUserDice userDice = session.rollDiceAgain(userId);
					
					// broadcast dice notification
					UserDiceNotification diceNoti = UserDiceNotification.newBuilder().addUserDice(userDice).build(); 
					NotificationUtils.broadcastUserDiceNotification(session, userId, diceNoti);
		
					// send success response with new rolled dice
					UseItemResponse useItemResponse = UseItemResponse.newBuilder()
						.setItemId(itemId)
						.addAllDices(userDice.getDicesList())
						.build();
					
					GameMessage response = GameMessage.newBuilder()
						.setCommand(GameCommandType.USE_ITEM_RESPONSE)
						.setMessageId(message.getMessageId())
						.setResultCode(resultCode)
						.setUseItemResponse(useItemResponse)
						.setUserId(userId)
						.build();
					sendResponse(response);	
				}
			}
			break;
			
		default:
			{
				
				// do nothing, just send response success 
				UseItemResponse useItemResponse = UseItemResponse.newBuilder()
					.setItemId(itemId)
					.build();

				GameMessage response = GameMessage.newBuilder()
					.setCommand(GameCommandType.USE_ITEM_RESPONSE)
					.setMessageId(message.getMessageId())
					.setResultCode(resultCode)
					.setUserId(userId)
					.setUseItemResponse(useItemResponse)
					.build();
				sendResponse(response);	
			}
			break;
		}
		
		// send to all other users in the session for use item request
		if (resultCode == GameResultCode.SUCCESS){
			NotificationUtils.broadcastNotification(session, userId, message);
		}
	}

	@Override
	public boolean isProcessForSessionAllocation() {
		return false;
	}

	@Override
	public boolean isProcessIgnoreSession() {
		return false;
	}

	@Override
	public boolean isProcessInStateMachine() {
		return false;
	}

}
