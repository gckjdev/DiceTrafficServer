package com.orange.game.dice.messagehandler.item;

import org.jboss.netty.channel.Channel;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UseItemResponse.Builder;

public class IncTimeItemHandler implements ItemHandleInterface {

	@Override
	public GameResultCode handleMessage(GameMessage message, Channel channel,
			DiceGameSession session, String userId, int itemId,
			Builder useItemResponseBuilder) {
		
		GameResultCode gameResultCode = GameResultCode.SUCCESS;
		
		int extendTime = message.getUseItemRequest().getExtendTime();
		if ( extendTime < 0 || extendTime > DiceGameStateMachineBuilder.WAIT_CLAIM_TIMEOUT) {
			gameResultCode = GameResultCode.ERROR_EXCESS_TIME_LIMIT;
		}
		
		int newInterval = session.getRemainTime(DiceGameAction.DiceTimerType.WAIT_CLAIM) + extendTime;
		ServerLog.info(session.getSessionId(), userId + " uses [IncTime] item, new interval is " + newInterval + " seconds");
		GameEventExecutor.getInstance().startTimer(session, newInterval, DiceGameAction.DiceTimerType.WAIT_CLAIM);
		
		return gameResultCode;
	}

}
