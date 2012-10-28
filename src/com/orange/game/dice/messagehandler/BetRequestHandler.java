package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
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
		
		String userId = message.getUserId();
		BetDiceRequest betDiceRequest = message.getBetDiceRequest();
		GameResultCode resultCode = GameResultCode.SUCCESS;
		
		if ( session.getRemainTime(DiceGameAction.DiceTimerType.WAIT_USER_BET) == -1 ) {
			resultCode = GameResultCode.ERROR_EXCESS_TIME_LIMIT;
		} else {
			((DiceGameSession)session).recordUserBet(userId, betDiceRequest);
			((DiceGameSession)session).incUserBetCount();
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
		
		// if all players have bet, we can move on to next state
		if ( ((DiceGameSession)session).getUserBetCount() == ((DiceGameSession)session).getPlayUserCount() -2 ) {
			GameEventExecutor.getInstance().fireAndDispatchEvent(GameCommandType.LOCAL_ALL_USER_BET, session.getSessionId(), userId);
		}
	}

}
