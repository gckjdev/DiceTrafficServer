package com.orange.game.dice.robot.client;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

import com.mongodb.DBObject;
import com.orange.common.log.ServerLog;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.game.constants.DBConstants;
import com.orange.game.constants.ServiceConstant;
import com.orange.game.model.dao.Item;
import com.orange.game.model.dao.User;
import com.orange.game.model.manager.UserManager;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameChatRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser;
import com.orange.network.game.protocol.model.GameBasicProtos.PBKeyValue;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser.Builder;

public class DiceRobotClient extends AbstractRobotClient {

	private final static Logger logger = Logger.getLogger(DiceRobotClient.class.getName());
	
	String openUserId = null;
	String callUserId = null;
	int callDice = -1;
	int callDiceNum = -1;
	boolean callDiceIsWild = false;
	private int callUserSeatId = -1;
	private boolean canOpenDice = false;
	private int playerCount = 0;
	private boolean robotWinThisGame = false;
	private boolean firstRound = true;
	
	// for itemType
	private final static int ITEM_TYPE_DICE_MIN = 2500;
	private final static int ITEM_TYPE_DICE_MAX = 2512;
	
	// chatContent type
	private final static int TEXT = 1;
	private final static int EXPRESSION = 2;
	
	private final static int IDX_CONTENT = 0;
	private final static int IDX_CONTENTID = 1;
	private final static int IDX_CONTNET_TYPE = 2;

	// What dices robot gets
	int[] robotRollResult={0,0,0,0,0,0};
	
	DiceRobotChatContent chatContent = DiceRobotChatContent.getInstance();
	
	List<PBUserDice> pbUserDiceList = null;
	List<PBDice> pbDiceList = null;
	
	ScheduledFuture<?> callDiceFuture = null;
	ScheduledFuture<?> openDiceFuture = null;
	
	ScheduledFuture<?> rollEndChatFuture = null;
	volatile static int rollEndChatLock = 2;
	private ScheduledFuture<?> chatFuture = null;
	
	int ruleType = GameEventExecutor.getInstance().getSessionManager().getRuleType();
	DiceRobotIntelligence diceRobotIntelligence = new DiceRobotIntelligence(ruleType);
	DiceRobotChatContent diceRobotChatContent = DiceRobotChatContent.getInstance();
	
	
	public DiceRobotClient(User user, int sessionId, int index) {
		super(user, sessionId,index);
		oldExp = experience = user.getExpByAppId(DBConstants.APPID_DICE);
		level = user.getLevelByAppId(DBConstants.APPID_DICE); 
		balance = user.getBalance();
//		dbclient = new MongoDBClient(DBConstants.D_GAME);
	}
	
	@Override
	public void handleMessage(GameMessage message){
		
		switch (message.getCommand()){
		
		case ROLL_DICE_BEGIN_NOTIFICATION_REQUEST:
			ServerLog.info(sessionId, "Robot "+nickName+" receive ROLL_DICE_BEGIN_NOTIFICATION_REQUEST");
			// Balance and reset data of current game
			if ( !firstRound ) {
				diceRobotIntelligence.balanceAndReset(userList.size(), robotWinThisGame);
			}
			break;
		
		case ROLL_DICE_END_NOTIFICATION_REQUEST:			
			ServerLog.info(sessionId, "Robot "+nickName+" receive ROLL_DICE_END_NOTIFICATION_REQUEST");
			if ( (pbUserDiceList = message.getRollDiceEndNotificationRequest().getUserDiceList() ) == null ) 
					throw new NullPointerException() ;
			else {
				for ( PBUserDice userDice : pbUserDiceList ) {
					if ( userDice.getUserId() != null && userDice.getUserId().equals(userId)) {
						pbDiceList = userDice.getDicesList();
						break;
					}
				}
				for ( int i = 0 ; i < 5; i++ ) {
					robotRollResult[i] = pbDiceList.get(i).getDice();
				}
			}
			diceRobotIntelligence.introspectRobotDices(robotRollResult);
			break;
			
		case NEXT_PLAYER_START_NOTIFICATION_REQUEST:
			ServerLog.info(sessionId, "Robot "+nickName+" receive NEXT_PLAYER_START_NOTIFICATION_REQUEST");
			if (message.getCurrentPlayUserId().equals(userId)){
				
				if ( (callUserId != null && this.sessionRealUserCount() == 0) || canOpenDice ){
					int multiple = 1;
					ServerLog.info(sessionId, "[NEXT_PLAYER_START_NOTIFICATION_REQUEST] robot dicides to open.");
					if ( diceRobotIntelligence.hasSetChat()) {
						sendChat(diceRobotIntelligence.getChatContent());
						diceRobotIntelligence.resetHasSetChat();
					}
					if (diceRobotIntelligence.getCanCut()) { 
						multiple = 2;
					}
					scheduleSendOpenDice(0, multiple);
				}
				else {
					// Make a decision what to call.
					playerCount = userList.size();
					diceRobotIntelligence.decideWhatToCall(nickName, playerCount, callDiceNum, callDice, callDiceIsWild);
					// Check the decision.
					if (diceRobotIntelligence.giveUpCall()) {
						ServerLog.info(sessionId, "[NEXT_PLAYER_START_NOTIFICATION_REQUEST] robot gives up call ,just open.");
						if ( diceRobotIntelligence.hasSetChat()) {
							sendChat(diceRobotIntelligence.getChatContent());
							diceRobotIntelligence.resetHasSetChat();
						}
						scheduleSendOpenDice(0, 1); 
					} else {
						scheduleSendCallDice(diceRobotIntelligence.getWhatTocall());
						if ( diceRobotIntelligence.hasSetChat()) {
							sendChat(diceRobotIntelligence.getChatContent());
							diceRobotIntelligence.resetHasSetChat();
						}
					}
				}
			}
			// 抢开
			else if ( canOpenDice ) {
					boolean canCut = diceRobotIntelligence.getCanCut();
					int multiple = (canCut == true ? 2 :1);
					ServerLog.info(sessionId, "!!!!!The callUserSeatId is " + callUserSeatId + ", Robot "+nickName
							+ "'s seatId is " + userList.get(userId).getSeatId());
					ServerLog.info(sessionId, "[CALL_DICE_RUQUET] *****Robot " + nickName + "rush to open!!!*****");
					sendOpenDice(1, multiple);
			}
			break;
			
		case CALL_DICE_REQUEST:
			callUserId = message.getUserId();
			callDice = message.getCallDiceRequest().getDice();
			if ( callDice < 1 || callDice > 6) {
				ServerLog.info(sessionId, "Error: <CALL_DICE_REQUEST>: dice face value is illegal:"+callDice);
				callDice = 1;
			}
			callDiceNum = message.getCallDiceRequest().getNum();
			if (message.getCallDiceRequest().hasWilds()){
				callDiceIsWild = message.getCallDiceRequest().getWilds();
			} else {
				callDiceIsWild = false;
			}
			// Get the callUser's seatId
			callUserSeatId = userList.get(callUserId).getSeatId();
			playerCount = userList.size();
////			ServerLog.info(sessionId, "Robot " + nickName + " receive CALL_DICE_REQUEST");
////			ServerLog.info(sessionId, "The playerCount is " + playerCount + ", seatId is " + callUserSeatId 
//						+". Robot " + nickName + "'s seatId is " + userList.get(userId).getSeatId());
			canOpenDice = diceRobotIntelligence.canOpenDice(nickName, playerCount,callUserId, callDiceNum, callDice, callDiceIsWild);
			break;
			
		case OPEN_DICE_REQUEST:
			// Only the callUser who challenged by others will randomly fire expression sendings
			if ( callUserId != null && callUserId.equals(userId) && RandomUtils.nextInt(2) == 1 ) {
				scheduleSendChat(chatFuture , 1);
			}
			openUserId = message.getUserId();
			ServerLog.info(sessionId, "Robot "+nickName+" receive OPEN_DICE_REQUEST");
			
			break;
			
		default:
			break;
		}
	}

	private void scheduleSendCallDice(final int[] whatToCall) {
		
		if (callDiceFuture != null){
			callDiceFuture.cancel(false);
		}
		
		callDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendCallDice(whatToCall);
			}
		}, 
		RandomUtils.nextInt(5)+1, TimeUnit.SECONDS);
	}

	private void sendCallDice(final int[] whatToCall) {
		
		
		int diceNum = whatToCall[0];
		int dice = whatToCall[1];
		boolean isWild = (whatToCall[2] == 1 ? true : false);
		
		// send call dice request here
		CallDiceRequest request = CallDiceRequest.newBuilder()
			.setDice(dice)
			.setNum(diceNum)
			.setWilds(isWild)
			.build();

		GameMessage message = GameMessage.newBuilder()
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.CALL_DICE_REQUEST)
			.setSessionId(sessionId)
			.setUserId(userId)
			.setCallDiceRequest(request)
			.build();
		
		send(message);
	}

	
	private void scheduleSendOpenDice(final int openType, final int multiple) {
		
		if (openDiceFuture != null){
			openDiceFuture.cancel(false);
		}
		
		openDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendOpenDice(openType, multiple);
			}
		}, 
		RandomUtils.nextInt(2)+1, TimeUnit.SECONDS);
	}

	private void sendOpenDice(int openType, int multiple) {
		ServerLog.info(sessionId, "Robot "+nickName+" open dice");
		
		OpenDiceRequest request = OpenDiceRequest.newBuilder()
				.setOpenType(openType)
				.setMultiple(multiple)
				.build();
		GameMessage message = GameMessage.newBuilder()
			.setOpenDiceRequest(request)
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.OPEN_DICE_REQUEST)
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);		
	}
	
	
	
	public void sendChat(final String[] content) {
		
		// index IDX_CONTENT(0) : content(only valid for TEXT)
		// index IDX_CONTENTID(1) : content voiceId or expressionId, depent on contentType
		// index IDX_CONTENT_TYPE(2) : contentType, TEXT or EXPRESSION
		String chatContent = content[IDX_CONTENT];
		String contentId = content[IDX_CONTENTID];
		int contentType = Integer.parseInt(content[IDX_CONTNET_TYPE]);
		
		ServerLog.info(sessionId, "Robot "+nickName+" sends chat content: " + contentId);
		
		GameChatRequest request = null;
		
		GameChatRequest.Builder builder = GameChatRequest.newBuilder()
				.setContentType(contentType) // 1: text, 2: expression
				.setContent(chatContent); // will be ignored when contentType is 2
		if ( contentType == TEXT ) {
				builder.setContentVoiceId(contentId);
		} else {
				builder.setExpressionId(contentId);
		}
		
		request = builder.build();
				
		GameMessage message = GameMessage.newBuilder()
			.setChatRequest(request)
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.CHAT_REQUEST)
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		ServerLog.info(sessionId, "<DiceRobotChatContent.sendChat()>Robot "+nickName+ " sends "+message.getCommand());
		send(message);		
	}
	
	
	public void scheduleSendChat(ScheduledFuture<?> chatFuture, int delay) {
		
		if (chatFuture != null){
			chatFuture.cancel(false);
		}
		
		// index 0: contentType
		// index 1: content( only valid for TEXT)
		// index 2: contentVoiceId or expressionId,depent on contentType
		
		chatFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				String[] tmp = {null, null};
				tmp = chatContent.getExpressionByMeaning("NEGATIVE");
				String[] expression = {null, null, Integer.toString(EXPRESSION)};
				expression[0] = tmp[0];
				expression[1] = tmp[1];
				sendChat(expression);
			}
		}, 
		delay, TimeUnit.SECONDS);
		
	}
	

	@Override
	public void resetPlayData(boolean robotWinThisGame) {
		
		openUserId = null;
		callUserId = null;
		callDice = -1;
		callDiceNum = -1;
		callDiceIsWild = false;
		callUserSeatId = -1;
		
		canOpenDice = false;
		pbUserDiceList = null;
		pbDiceList = null;
		
		openDiceFuture = null;
		chatFuture = null;
		
		this.robotWinThisGame = robotWinThisGame;
		firstRound = false;
		rollEndChatLock = 2;
	}

	
	@Override
	public String getAppId() {
		return DBConstants.APPID_DICE;
	}

	public String getGameId(){
		return DBConstants.GAME_ID_DICE;
	}
	
	@Override
	public PBGameUser toPBGameUserSpecificPart(Builder builder) {
		
		List<Item> items = user.getItems();
		if ( items.size() > 0 ) {
			int diceItemType = ITEM_TYPE_DICE_MIN;
			for ( Item item: items ) {
				int itemType = item.getItemType();
				if ( itemType > ITEM_TYPE_DICE_MIN && itemType < ITEM_TYPE_DICE_MAX ) {
					diceItemType = item.getItemType();
					break;
				}
			}
			PBKeyValue pbKeyValue = PBKeyValue.newBuilder()
					.setName("CUSTOM_DICE")
					.setValue(Integer.toString(diceItemType-2500)) // should substract by 2500, required by the client
					.build();
			
			builder.addAttributes(pbKeyValue);
			logger.info("<DiceRobotClient.toPBGameUserSpecificPart> Robot["+ nickName+"] adds a dice item, itemType is " + diceItemType);
		}
		
		return builder.build();
	}
	

}
