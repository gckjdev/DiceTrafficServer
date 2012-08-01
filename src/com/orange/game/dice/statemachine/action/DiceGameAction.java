package com.orange.game.dice.statemachine.action;

import com.orange.common.statemachine.Action;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCompleteReason;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceBeginNotificationRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceEndNotificationRequest;

public class DiceGameAction{

//	public static class BroadcastDrawUserChange implements Action {
	//
	//		@Override
	//		public void execute(Object context) {
	//			GameSession session = (GameSession)context;
	//			GameNotification.broadcastDrawUserChangeNotification(session);
	//		}
	//
	//	}
	//
	//	public static class SelectDrawUserIfNone implements Action {
	//
	//		@Override
	//		public void execute(Object context) {
	//			GameSession session = (GameSession)context;
	//			if (session.getCurrentPlayUser() == null){
	//				sessionManager.selectCurrentPlayer(session);
	//			}
	//		}
	//
	//	}
	//
	//	public static class ClearRobotTimer implements Action {
	//
	//		@Override
	//		public void execute(Object context) {
	//			GameSession session = (GameSession)context;
	//			session.clearRobotTimer();
	//		}
	//
	//	}
	//
	//	public static class CalculateDrawUserCoins implements Action {
	//
	//		@Override
	//		public void execute(Object context) {
	//			GameSession session = (GameSession)context;
	//			session.calculateDrawUserCoins();
	//		}
	//
	//	}
	//
	//	public static final GameSessionUserManager sessionUserManager = GameSessionUserManager.getInstance();
	//	public static final GameSessionManager sessionManager = GameSessionManager.getInstance();
	//


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

//
//	public static class PrepareRobot implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			sessionManager.prepareRobotTimer(session);
//		}
//
//	}
//	
//	public static class StartGame implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			session.startGame();
//		}
//
//	}
//
//
//	public static class CompleteGame implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			sessionUserManager.clearUserPlaying(session);
//			session.completeTurn();			
//			GameNotification.broadcastNotification(session, null, GameCommandType.GAME_TURN_COMPLETE_NOTIFICATION_REQUEST);
//
//			sessionManager.adjustSessionSetForTurnComplete(session);			
//		}
//
//	}	
//
//	public static class KickDrawUser implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			com.orange.gameserver.draw.dao.User user = session.getCurrentPlayUser();
//			if (user != null){
//				GameSessionManager.getInstance().userQuitSession(user.getUserId(), session, false);
//				ChannelUserManager.getInstance().processDisconnectChannel(user.getChannel());
//			}
//		}
//
//	}
//	
//
//
//
//
//	public static class PlayGame implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			
//			// TODO think about it
//		}
//
//	}
//	public static class ClearTimer implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			session.clearTimer();
//		}
//
//	}
//
//	public static class SetWaitPickWordTimer implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			GameService.getInstance().startTimer(session, 
//					PICK_WORD_TIMEOUT, GameSession.TimerType.PICK_WORD);
//		}
//
//	}
//

//
//	public static class SetOneUserWaitTimer implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			GameService.getInstance().startTimer(session, 
//					USER_WAIT_TIMEOUT, GameSession.TimerType.USER_WAIT);
//		}
//
//	}
//	
//	public static class SetDrawGuessTimer implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			GameService.getInstance().startTimer(session, 
//					DRAW_GUESS_TIMEOUT, GameSession.TimerType.DRAW_GUESS);
//		}
//
//	}
//
//	public static class SelectDrawUser implements Action {
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			GameSessionManager.getInstance().selectCurrentPlayer(session);
//		}
//
//	}

//	public static class InitGame implements Action{
//
//		@Override
//		public void execute(Object context) {
//			GameSession session = (GameSession)context;
//			session.resetGame();
//		}
//		
//	}

}
