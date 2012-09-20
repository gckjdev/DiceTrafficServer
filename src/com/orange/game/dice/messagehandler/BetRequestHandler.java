package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;
import org.omg.CORBA.PRIVATE_MEMBER;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.BetDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class BetRequestHandler extends AbstractMessageHandler {

	public BetRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
	}

	@Override
	public boolean isProcessIgnoreSession() {
		return false;
	}

	@Override
	public boolean isProcessInStateMachine() {
		return false;
	}

	@Override
	public boolean isProcessForSessionAllocation() {
		return false;
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel,
			GameSession session) {

		ServerLog.info(session.getSessionId(), "<BetRequestHandler> Receive bet request from "
					+ message.getUserId());
		final int MAX_TIME_LIMIT = 4;
		
		String userId = message.getUserId();
		BetDiceRequest betDiceRequest = message.getBetDiceRequest();
		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		if ( session.getRemainTime(DiceGameAction.DiceTimerType.WAIT_USER_BET) == -1 ) {
			resultCode = GameResultCode.ERROR_EXCESS_TIME_LIMIT;
		} else {
			((DiceGameSession)session).recordUserBet(userId, betDiceRequest);
			ServerLog.info(session.getSessionId(), "the odds is " + betDiceRequest.getOdds()
					+ " , the ante is "+ betDiceRequest.getAnte());
		}
		
		GameMessage response = GameMessage.newBuilder()
				.setCommand(GameCommandType.BET_DICE_RESPONSE)
				.setMessageId(message.getMessageId())
				.setResultCode(resultCode)
				.build();
		sendResponse(response);
		
		if (resultCode == GameResultCode.SUCCESS){
			// broadcast call dice		
			NotificationUtils.broadcastBetNotification(session, betDiceRequest,userId, false);
		}
	}

}
