package com.orange.game.dice.statemachine.action;

import java.util.Collections;
import java.util.List;
import com.orange.common.log.ServerLog;
import com.orange.common.statemachine.Action;
import com.orange.game.constants.DBConstants;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.game.traffic.service.SessionUserService;
import com.orange.game.traffic.service.UserGameResultService;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.GameOverNotificationRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceEndNotificationRequest;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceFinalCount;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceGameResult;

public class DiceGameAction{

	public enum DiceTimerType{
		START, ROLL_DICE, WAIT_CLAIM, SHOW_RESULT, TAKEN_OVER_USER_WAIT, WAIT_USER_BET,
	};

	public static class KickWaitTimeOutUsers implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			
			List<String> timeOutUsersList = session.getWaitTimeOutUsers();
			ServerLog.info(session.getSessionId(), "<KickWaitTimeOutUsers> users="+timeOutUsersList.toString());
			for (String userId : timeOutUsersList){
				SessionUserService.getInstance().removeUser(session, userId, true);
				session.clearWaitClaimTimeOutTimes(userId);
			}
		}

	}
	
	public static class SetWaitClaimTimer implements Action {

		int interval;
		final Object timerType;
		final int normalInterval;
		
		public SetWaitClaimTimer(int interval, Object timerType){
			this.interval = interval;
			this.timerType = timerType;
			this.normalInterval = interval;
		}
		
		private void  getNewInterval(GameSession session) {
			int newInterval = 0;
			if ((newInterval = session.getNewInterval()) != 0) {
				interval = newInterval; 
			}
			interval = normalInterval;
			
		}
		
		private void clearNewInterval(GameSession session) {
			session.setNewInternal(0);
		}
		
		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			// check to see if the interval is changed(by last user using decTime item)
			getNewInterval(session);
			GameEventExecutor.getInstance().startTimer(session, interval, timerType);
			// clear it, so the intervel won't influence next user.
			clearNewInterval(session);
			// correctly set the  decreaseTimeForNextPlayUser 
			if ( session.getDecreaseTimeForNextPlayUser() == true ) {
				session.setDecreaseTimeForNextPlayUser(false);
			}
		}
	}

	
	public static class ClearWaitClaimTimeOutTimes implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			session.clearWaitClaimTimeOutTimes(session.getCurrentPlayUserId());
		}

	}

	
	public static class SetShowResultTimer implements Action {

		private static final int SHOW_RESULT_SECONDS_PER_USER = 4;
		private static final int SHOW_COINS_SECONDS = 3;
		private static final int EXTRA_SECONDS = 0;

		@Override
		public void execute(Object context) {
			// TODO Auto-generated method stub
			DiceGameSession session = (DiceGameSession)context;
			int timeOut = session.getPlayUserCount()*SHOW_RESULT_SECONDS_PER_USER + SHOW_COINS_SECONDS + EXTRA_SECONDS;
			GameEventExecutor.getInstance().startTimer(session, 
					timeOut, DiceTimerType.SHOW_RESULT);

		}

	}
	public class CallDiceForTakenOverUser implements Action {

		@Override
		public void execute(Object context) {
			// TODO Auto-generated method stub

		}

	}

	public static class KickTakenOverUser implements Action {

		@Override
		public void execute(Object context) {
			// kick all user which are taken over
			DiceGameSession session = (DiceGameSession)context;
			SessionUserService.getInstance().kickTakenOverUser(session);
		}

	}
	public static class SelectLoserAsCurrentPlayerUser implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			String loserUserId = session.getLoserUserId();
			if (loserUserId == null){
				session.selectPlayerUser();
			}
			else{
				ServerLog.info(session.getSessionId(), "try to set loser "+loserUserId+" as current play user");
				int loserUserIndex = session.getUserIndex(loserUserId);
				if (loserUserIndex == -1){
					// loser user doesn't exist
					session.selectPlayerUser();
				}
				else{
					session.setCurrentPlayUser(loserUserIndex);
				}
			}
			
		}

	}
	public static GameResultCode openDiceAndBroadcast(DiceGameSession session,
			String userId, int openType, int openMultiple) {
		GameResultCode resultCode = session.openDice(userId, openType, openMultiple);
		if (resultCode == GameResultCode.SUCCESS){
			
			OpenDiceRequest request = OpenDiceRequest.newBuilder()
				.setOpenType(openType)
				.setMultiple(openMultiple)
				.build();
			
			GameMessageProtos.GameMessage.Builder builder = GameMessageProtos.GameMessage.newBuilder()
				.setCommand(GameCommandType.OPEN_DICE_REQUEST)
				.setMessageId(GameEventExecutor.getInstance().generateMessageId())
				.setSessionId(session.getSessionId())
				.setUserId(userId)
				.setOpenDiceRequest(request);

			NotificationUtils.broadcastNotification(session, builder.build());
		}
		
		return resultCode;
	}
	
	public static GameResultCode openDiceAndBroadcast(DiceGameSession session,
			String userId) {
		int openType = DiceGameSession.DICE_OPEN_TYPE_NORMAL;
		int openMultiple = 1;
		return openDiceAndBroadcast(session, userId, openType, openMultiple);
	}
	
	public static class AutoCallOrOpen implements Action{
		
		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			
			String currentPlayUserId = session.getCurrentPlayUserId();
			String callDiceUserId = session.getCallDiceUserId();
			int currentDiceNum = session.getCurrentDiceNum();
			int currentDice = session.getCurrentDice();			
			int sessionId = session.getSessionId();
			
			if (currentPlayUserId == null){
				ServerLog.warn(sessionId, "<autoCallOrOpen> but current play user Id is null");
				return;
			}
			
			if (callDiceUserId != null && callDiceUserId.equals(currentPlayUserId)){
				ServerLog.warn(sessionId, "<autoCallOrOpen> but callDiceUserId is already current user");
				return;			
			}
			
			session.incWaitClaimTimeOutTimes(currentPlayUserId);
			
			GameResultCode resultCode = GameResultCode.SUCCESS;
			if (session.canContinueCall()){			
				boolean wilds = session.getIsWilds();
				if (callDiceUserId == null){
					resultCode = session.callDice(currentPlayUserId, session.getPlayUserCount()+1, DiceGameSession.DICE_2, false); 
				}				
				else if (session.reachMaxDice(currentDiceNum)){
					if (currentDice == DiceGameSession.DICE_6){
						resultCode = session.callDice(currentPlayUserId, currentDiceNum, DiceGameSession.DICE_1, true);
					}
					else{
						resultCode = session.callDice(currentPlayUserId, currentDiceNum, currentDice+1, wilds);
					}															
				}
				else{
					resultCode = session.callDice(currentPlayUserId, currentDiceNum+1, currentDice, wilds);					
				}				
				
				if (resultCode == GameResultCode.SUCCESS){		
					CallDiceRequest request = CallDiceRequest.newBuilder()
						.setDice(session.getCurrentDice())
						.setNum(session.getCurrentDiceNum())
						.setWilds(session.getIsWilds())
						.build();
					NotificationUtils.broadcastCallDiceNotification(session, request, true);
				}
			}
			else {				
				resultCode = openDiceAndBroadcast(session, currentPlayUserId);
				if (resultCode != GameResultCode.SUCCESS){
					ServerLog.warn(sessionId, "<AutoCallOrOpen> but fail to open, result code ="+resultCode.toString());
				}
			}				
		}
	}
	
	public static class DirectOpenDice implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			openDiceAndBroadcast(session, session.getCurrentPlayUserId());
		}

	}
	public static class BroadcastNextPlayerNotification implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			NotificationUtils.broadcastNotification(session, null, GameCommandType.NEXT_PLAYER_START_NOTIFICATION_REQUEST);			
		}

	}
	public static class BroadcastRollDiceBegin implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			NotificationUtils.broadcastNotification(session, null, GameCommandType.ROLL_DICE_BEGIN_NOTIFICATION_REQUEST);
		}

	}
	public static class RollDiceAndBroadcast implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			
			session.rollDice();
			
			// send notification for the user
			RollDiceEndNotificationRequest notification =RollDiceEndNotificationRequest.newBuilder()
				.addAllUserDice(session.getUserDices())
				.build();
			
			GameMessageProtos.GameMessage message = GameMessageProtos.GameMessage.newBuilder()
				.setCommand(GameCommandType.ROLL_DICE_END_NOTIFICATION_REQUEST)
				.setMessageId(GameEventExecutor.getInstance().generateMessageId())
				.setSessionId(session.getSessionId())
				.setRollDiceEndNotificationRequest(notification)							
				.build();
			
			NotificationUtils.broadcastNotification(session, null, message);
		}

	}
	
	public static class CompleteGame implements Action {

		private UserGameResultService service = UserGameResultService.getInstance();
		
		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			
			int ruleType = session.getRuleType();
			
			// all users' dices settlement
			List<PBDiceFinalCount> diceFinalCountList = session.diceCountSettlement(ruleType);
			
			// calculate how many coins that users gain
			if ( ! diceFinalCountList.equals(Collections.emptyList()) && diceFinalCountList.size() >= 2 ) { // only meaningful for at least 2 users
				int allFinalCount = 0 ; // all user total final count
				for ( PBDiceFinalCount finalCount: diceFinalCountList ) {
					allFinalCount += finalCount.getFinalDiceCount();
				}
				session.calculateCoins(allFinalCount,ruleType );
			}
			
			// write game result(playtimes, wintime, losetimes, etc) into db
			service.writeAllUserGameResultIntoDB(session, DBConstants.GAME_ID_DICE);
			
			// charge/deduct coins
			service.writeAllUserCoinsIntoDB(session,DBConstants.C_CHARGE_SOURCE_DICE_WIN);
			
			// broadcast complete complete with result
			PBDiceGameResult result = PBDiceGameResult.newBuilder()
				.addAllUserResult(session.getUserResults())
				.addAllFinalCount(diceFinalCountList)
				.build();
				
			GameOverNotificationRequest notification = GameOverNotificationRequest.newBuilder()
				.setGameResult(result)
				.build();
			
			GameMessageProtos.GameMessage.Builder builder = GameMessageProtos.GameMessage.newBuilder()
				.setCommand(GameCommandType.GAME_OVER_NOTIFICATION_REQUEST)
				.setMessageId(GameEventExecutor.getInstance().generateMessageId())
				.setSessionId(session.getSessionId())
				.setGameOverNotificationRequest(notification);				
			
			if (session.getCurrentPlayUserId() != null){
				builder.setCurrentPlayUserId(session.getCurrentPlayUserId());
			}
		
			GameMessage message = builder.build();
			ServerLog.info(session.getSessionId(), "send game over="+message.toString());
			NotificationUtils.broadcastNotification(session, null, message);
				
		}

	}

	public static class RestartGame implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			session.restartGame();
			SessionUserService.getInstance().kickTakenOverUser(session);			
			return;
		}
	}		

}
