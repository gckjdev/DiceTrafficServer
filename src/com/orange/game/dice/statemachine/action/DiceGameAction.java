package com.orange.game.dice.statemachine.action;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.cli.CliParser.newColumnFamily_return;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.orange.common.log.ServerLog;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.statemachine.Action;
import com.orange.game.constants.DBConstants;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.model.manager.UserManager;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameSessionUserList;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.robot.client.RobotService;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.game.traffic.service.GameDBService;
import com.orange.game.traffic.service.SessionUserService;
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
import com.orange.network.game.protocol.model.DiceProtos.PBUserResult;

public class DiceGameAction{

	public enum DiceTimerType{
		START, ROLL_DICE, WAIT_CLAIM, SHOW_RESULT, TAKEN_OVER_USER_WAIT
	};
	
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
	public static class ClearAllUserPlaying implements Action {

		@Override
		public void execute(Object context) {
			// make all user not playing
			DiceGameSession session = (DiceGameSession)context;
			session.getUserList().clearAllUserPlaying();
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
	
	public static void openDiceAndBroadcast(DiceGameSession session,
			String userId) {
		int openType = DiceGameSession.DICE_OPEN_TYPE_NORMAL;
		int openMultiple = 1;
		openDiceAndBroadcast(session, userId, openType, openMultiple);
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
			else if (session.canOpen(currentPlayUserId)){				
				openDiceAndBroadcast(session, currentPlayUserId);				
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

		@Override
		public void execute(Object context) {
			DiceGameSession session = (DiceGameSession)context;
						
			// calcuate user gain conins
			session.calculateCoins();
			
			// save result
			saveUserResultIntoDB(session);
			
			// charge/deduct coins
			writeUserCoinsIntoDB(session);
			
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
				
		}

		private void writeUserCoinsIntoDB(final DiceGameSession session) {
			final GameDBService dbService = GameDBService.getInstance();
			dbService.executeDBRequest(session.getSessionId()	, new Runnable() {
				
				@Override
				public void run() {
					
					MongoDBClient dbClient = dbService.getMongoDBClient(session.getSessionId());
					
					Collection<PBUserResult> resultList = session.getUserResults();
					for (PBUserResult result : resultList){
						boolean win = result.getWin();
						String userId = result.getUserId();
						int amount = result.getGainCoins();
						
						if (win){
							UserManager.chargeAccount(dbClient, userId, amount, DBConstants.C_CHARGE_SOURCE_DICE_WIN, null, null);
						}
						else{
							UserManager.deductAccount(dbClient, userId, -amount, DBConstants.C_CHARGE_SOURCE_DICE_WIN);
						}
					}
				}
			});
			
		}

		private void saveUserResultIntoDB(final DiceGameSession session) {
			final GameDBService dbService = GameDBService.getInstance();
			dbService.executeDBRequest(session.getSessionId()	, new Runnable() {
				
				@Override
				public void run() {
					
					MongoDBClient dbClient = dbService.getMongoDBClient(session.getSessionId());
 					String tableName = DBConstants.T_USER_GAME_RESULT;
					Collection<PBUserResult> resultList = session.getUserResults();
					List<GameUser> gameUserList = session.getUserList().getUserList();
					String[] getUserResultUserId = { null, null};
					Map<String, Boolean> gameResult = new HashMap<String, Boolean>();
					int count = 0;
					
					// get userResult, only two, one wins, one loses.
					for ( PBUserResult result : resultList ) {
						if ( count < 2 ) {
							getUserResultUserId[count] = result.getUserId(); 
							gameResult.put(getUserResultUserId[count], new Boolean(result.getWin()));
							count++;
						}
					}
					
					// correctly set count
					count = 2;
					
					// update each player
					for(GameUser gameUser : gameUserList) {
						if ( gameUser.isPlaying() == true ) {
							String userId = gameUser.getUserId();
							// query by user_id and game_id
							DBObject query = new BasicDBObject();
							query.put(DBConstants.F_USERID, userId);
							query.put(DBConstants.F_GAMEID, "Dice");
						
							// update
							DBObject update = new BasicDBObject();
							DBObject incUpdate = new BasicDBObject();
							DBObject dateUpdate = new BasicDBObject();
							
							incUpdate.put(DBConstants.F_PLAY_TIMES, 1);
							// check count first to eliminate unnecessary comparison of userId
							if ( count > 0 && 
									userId.equals(getUserResultUserId[0]) || userId.equals(getUserResultUserId[1]) ) {
								if ( gameResult.get(userId) == true ) { // wins
									incUpdate.put(DBConstants.F_WIN_TIMES, 1);
								} 
								else { // loses
									incUpdate.put(DBConstants.F_LOSE_TIMES, 1);
								}
								count--;
							}
							dateUpdate.put(DBConstants.F_MODIFY_DATE, new Date());
						
							update.put("$inc", incUpdate);
							update.put("$set", dateUpdate);
						
							dbClient.upsertAll(tableName, query, update);
							}
					}
					
				}
			});
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
