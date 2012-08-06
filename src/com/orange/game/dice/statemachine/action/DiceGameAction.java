package com.orange.game.dice.statemachine.action;

import com.orange.common.log.ServerLog;
import com.orange.common.statemachine.Action;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCompleteReason;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.GameOverNotificationRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceBeginNotificationRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceEndNotificationRequest;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceGameResult;

public class DiceGameAction{

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
					session.setCurrentPlayUser(loserUserId, loserUserIndex);
				}
			}
			
		}

	}
	public static class ClearRobotTimer implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			session.clearRobotTimer();
		}

	}
	public static class PrepareRobot implements Action {

		@Override
		public void execute(Object context) {
			GameSession session = (GameSession)context;
			GameEventExecutor.getInstance().prepareRobotTimer(session, RobotService.getInstance());
		}

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
			
			GameResultCode resultCode = GameResultCode.SUCCESS;
			if (session.canContinueCall()){			
				resultCode = session.callDice(currentPlayUserId, currentDiceNum+1, currentDice);
				
				// TODO here maybe increase currentDice instead of currentDiceNum
				
				if (resultCode == GameResultCode.SUCCESS){		
					CallDiceRequest request = CallDiceRequest.newBuilder()
						.setDice(session.getCurrentDice())
						.setNum(session.getCurrentDiceNum())
						.build();
					NotificationUtils.broadcastCallDiceNotification(session, request);
				}
			}
			else if (session.canOpen(currentPlayUserId)){
				resultCode = session.openDice(currentPlayUserId);
				if (resultCode == GameResultCode.SUCCESS){
					
					OpenDiceRequest request = OpenDiceRequest.newBuilder().setOpenType(0).build();
					
					GameMessageProtos.GameMessage.Builder builder = GameMessageProtos.GameMessage.newBuilder()
						.setCommand(GameCommandType.OPEN_DICE_REQUEST)
						.setMessageId(GameEventExecutor.getInstance().generateMessageId())
						.setSessionId(session.getSessionId())
						.setUserId(currentPlayUserId)
						.setOpenDiceRequest(request);

					NotificationUtils.broadcastNotification(session, builder.build());
				}
			}				
		}
	}
	
	public static class DirectOpenDice implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			session.openDice(session.getCurrentPlayUserId());
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

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			
			
			// make all user not playing
			session.getUserList().clearAllUserPlaying();

			// calcuate user gain conins
			session.calculateCoins();
			
			// broadcast complete complete with result
			PBDiceGameResult result = PBDiceGameResult.newBuilder()
				.addAllUserResult(session.getUserResults())
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

			// kick all user which are taken over
			GameEventExecutor.getInstance().kickTakenOverUser(session);
			
			// TODO 
			// sessionManager.adjustSessionSetForTurnComplete(session);			
		}

	}

	public static class RestartGame implements Action {

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
			session.restartGame();
			return;
		}
	}		

}
