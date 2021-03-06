package com.orange.game.dice.messagehandler;


import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.messagehandler.item.DecTimeItemHandler;
import com.orange.game.dice.messagehandler.item.DefaultItemHandler;
import com.orange.game.dice.messagehandler.item.DoubleCoinItemHandler;
import com.orange.game.dice.messagehandler.item.ItemHandleInterface;
import com.orange.game.dice.messagehandler.item.IncTimeItemHandler;
import com.orange.game.dice.messagehandler.item.ReverseCallItemHandler;
import com.orange.game.dice.messagehandler.item.RollDiceItemHandler;
import com.orange.game.dice.messagehandler.item.SkipCallItemHandler;
import com.orange.game.dice.model.DiceGameConstant;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse;

public class DiceUseItemRequestHandler extends AbstractMessageHandler {

	public static ItemHandleInterface doubleCoinItemHandler = new DoubleCoinItemHandler();
	public static ItemHandleInterface rollDiceItemHandler = new RollDiceItemHandler();
	public static ItemHandleInterface defaultItemHandler = new DefaultItemHandler();
	public static ItemHandleInterface reverseCallItemHandler = new ReverseCallItemHandler();
	public static ItemHandleInterface incTimeItemHandler = new IncTimeItemHandler();
	public static ItemHandleInterface decTimeItemHandler = new DecTimeItemHandler();
	public static ItemHandleInterface skipCallItemHandler = new SkipCallItemHandler();
//	public static ItemHandleInterface doubleKillItemHandler = new DoubleKillItemHandler();
	
	public DiceUseItemRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}


	public void handleRequest(GameMessage message, Channel channel,
			GameSession gameSession) {
		DiceGameSession session = (DiceGameSession)gameSession;
		
		if (session == null){
			ServerLog.warn(0, "<UseItem> but session is null");						
			return;
		}
		
		String userId = message.getUserId();
		if (userId == null){
			ServerLog.warn(session.getSessionId(), "<UseItem> but userId is null");						
			return;
		}
		
		UseItemRequest request = message.getUseItemRequest();
		if (request == null){
			ServerLog.warn(session.getSessionId(), "<UseItem> but item request is null");			
			return;
		}
		
		int itemId = request.getItemId();	
		
		GameResultCode resultCode = GameResultCode.SUCCESS;
		ItemHandleInterface itemHandler = getItemHandler(itemId);		
		
		// prepare response builder
		UseItemResponse.Builder useItemResponseBuilder = UseItemResponse.newBuilder()
			.setItemId(itemId);

		resultCode = itemHandler.handleMessage(message, channel, session, userId, itemId, useItemResponseBuilder);		
		ServerLog.info(session.getSessionId(), "<UseItem> itemId="+itemId+",result="+resultCode.toString());
		
		// send use item response
		int playDirection = session.getPlayDirection();
		String nextPlayerId = session.peekNextPlayerId();
		boolean decreaseTimerForNextPlayUser = session.getDecreaseTimeForNextPlayUser();
		
		UseItemResponse useItemResponse = useItemResponseBuilder
				.setItemId(itemId)
				.setDirection(playDirection)
				.setNextPlayUserId(nextPlayerId)
				.setDecreaseTimeForNextPlayUser(decreaseTimerForNextPlayUser)
				.build();		
		GameMessage response = GameMessage.newBuilder()
			.setCommand(GameCommandType.USE_ITEM_RESPONSE)
			.setMessageId(message.getMessageId())
			.setResultCode(resultCode)
			.setUseItemResponse(useItemResponse)
			.setUserId(userId)
			.build();
		sendResponse(response);	
		
		
		// broadcast to all other users in the session for use item request
		if (resultCode == GameResultCode.SUCCESS){
			UseItemRequest wrappedRequest = UseItemRequest.newBuilder(request)
					.setDirection(playDirection)
					.setNextPlayUserId(nextPlayerId)
					.setDecreaseTimeForNextPlayUser(decreaseTimerForNextPlayUser)
					.build();
			GameMessage wrappedMessage = GameMessage.newBuilder(message)
					.setUseItemRequest(wrappedRequest)
					.build();
			NotificationUtils.broadcastNotification(session, userId, wrappedMessage);
		}
	}   
	    
	private ItemHandleInterface getItemHandler(int itemId) {
		ItemHandleInterface itemHandler = null;
		switch (itemId){
			case DiceGameConstant.DICE_ITEM_ROLL_DICE_AGAIN:
				itemHandler = rollDiceItemHandler;
				break;

			case DiceGameConstant.DICE_ITEM_DOUBLE_COIN:
				itemHandler = doubleCoinItemHandler;
				break;

			case DiceGameConstant.Dice_PEEK:
				itemHandler = defaultItemHandler;
				break;

			case DiceGameConstant.Dice_REVERSE_CALL:
				itemHandler = reverseCallItemHandler;
				break;

			case DiceGameConstant.Dice_INC_TIME:
				itemHandler = incTimeItemHandler;
				break;

			case DiceGameConstant.Dice_DEC_TIME:
				itemHandler = decTimeItemHandler;
				break;

			case DiceGameConstant.Dice_CALL_HINT:
				itemHandler = defaultItemHandler;
				break;

			case DiceGameConstant.DICE_SKIP_CALL:
				itemHandler = skipCallItemHandler;
				break;

			case DiceGameConstant.Dice_DOUBLE_KILL:
//				itemHandler = doubleKillItemHandler;
				break;

			case DiceGameConstant.Dice_FLOWER:
				itemHandler = decTimeItemHandler;
				break;

			case DiceGameConstant.Dice_TOMATO:
				itemHandler = decTimeItemHandler;
				break;

			default:
				itemHandler = defaultItemHandler;
				break;
		}
		
		return itemHandler;
	}

	/*
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
			
		case DiceGameConstant.DICE_ITEM_DOUBLE_COIN:
		{
			if (session.isOpen()){
				resultCode = GameResultCode.ERROR_DICE_ALREADY_OPEN;											
			}
			else{
				int multiple = 2;
				resultCode = DiceGameAction.openDiceAndBroadcast(session, userId, DiceGameSession.DICE_OPEN_TYPE_CUT, multiple);
			}
						
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
			
			if (resultCode == GameResultCode.SUCCESS){
				// fire event
				GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_OPEN_DICE, session.getSessionId(), userId);				
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
	*/

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
