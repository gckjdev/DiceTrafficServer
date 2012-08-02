package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction.BroadcastNextPlayerNotification;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class CallDiceRequestHandler extends AbstractMessageHandler {

	public CallDiceRequestHandler(MessageEvent messageEvent) {
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

		CallDiceRequest request = message.getCallDiceRequest();
		int num = request.getNum();
		int dice = request.getDice();

		DiceGameSession diceSession = (DiceGameSession)session;
		diceSession.callDice(userId, num, dice);
		
		// broadcast call dice		
		NotificationUtils.broadcastCallDiceNotification(session, request);
		
		// fire event
		GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_CALL_DICE, session.getSessionId(), userId);
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
