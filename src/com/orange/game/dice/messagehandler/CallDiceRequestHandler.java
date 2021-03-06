package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class CallDiceRequestHandler extends AbstractMessageHandler {

	public CallDiceRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
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

		CallDiceRequest request = message.getCallDiceRequest();
		int num = request.getNum();
		int dice = request.getDice();
		boolean isWild = request.getWilds();

		DiceGameSession diceSession = (DiceGameSession)session;
		GameResultCode resultCode = diceSession.callDice(userId, num, dice, isWild);
		
		GameMessage response = GameMessage.newBuilder()
			.setCommand(GameCommandType.CALL_DICE_RESPONSE)
			.setMessageId(message.getMessageId())
			.setResultCode(resultCode)
			.setCallDiceRequest(request)
			.setUserId(userId)
			.build();
		sendResponse(response);
		
		if (resultCode == GameResultCode.SUCCESS){
			// broadcast call dice		
			NotificationUtils.broadcastCallDiceNotification(session, request, false);
			
			// fire event
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_CALL_DICE, session.getSessionId(), userId);
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
