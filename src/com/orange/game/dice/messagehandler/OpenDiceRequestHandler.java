package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.mime.qweibo.examples.QWeiboType.ResultType;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;

public class OpenDiceRequestHandler extends AbstractMessageHandler {

	public OpenDiceRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel, GameSession session) {
		if (session == null){
			return;
		}

		String userId = message.getUserId();
		if (userId == null){
			return;
		}

		OpenDiceRequest request = message.getOpenDiceRequest();

		DiceGameSession diceSession = (DiceGameSession)session;
		GameResultCode result = diceSession.openDice(userId, request.getOpenType(), request.getMultiple()); 
		
		GameMessage response = GameMessage.newBuilder()
			.setCommand(GameCommandType.OPEN_DICE_RESPONSE)
			.setMessageId(message.getMessageId())
			.setResultCode(result)
			.build();
		sendResponse(response);
		
		if (result == GameResultCode.SUCCESS){		
			// broadcast open dice		
			NotificationUtils.broadcastNotification(diceSession, userId, message);
		
			// fire event
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_OPEN_DICE, session.getSessionId(), userId);
		}
	}

	@Override
	public boolean isProcessForSessionAllocation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessIgnoreSession() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessInStateMachine() {
		// TODO Auto-generated method stub
		return false;
	}

}
