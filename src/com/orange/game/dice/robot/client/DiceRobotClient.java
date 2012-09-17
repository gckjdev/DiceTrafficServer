package com.orange.game.dice.robot.client;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;
import com.orange.common.log.ServerLog;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameChatRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;

public class DiceRobotClient extends AbstractRobotClient {

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
	
	DiceRobotIntelligence diceRobotIntelligence = new DiceRobotIntelligence();
	DiceRobotChatContent diceRobotChatContent = DiceRobotChatContent.getInstance();
	
	
	public DiceRobotClient(String userId, String nickName, String avatar,
			boolean gender, String location, int sessionId, int index) {
		super(userId, nickName, avatar, gender, location, sessionId, index);
		
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
				
				if ( this.sessionRealUserCount() == 0 || canOpenDice ){
					ServerLog.info(sessionId, "[NEXT_PLAYER_START_NOTIFICATION_REQUEST] robot dicides to open.");
					if ( diceRobotIntelligence.hasSetChat()) {
						sendChat(diceRobotIntelligence.getChatContent());
						diceRobotIntelligence.resetHasSetChat();
					}
					scheduleSendOpenDice(0);
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
						scheduleSendOpenDice(0);
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
					ServerLog.info(sessionId, "!!!!!The callUserSeatId is " + callUserSeatId + ", Robot "+nickName
							+ "'s seatId is " + userList.get(userId).getSeatId());
					ServerLog.info(sessionId, "[CALL_DICE_RUQUET] *****Robot " + nickName + "rush to open!!!*****");
					sendOpenDice(1);
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
			if (callUserId.equals(userId) && RandomUtils.nextInt(2) == 1 ) {
				if ( RandomUtils.nextInt(2) == 1 ) {
					String[] tmp = {null, null};
					tmp = chatContent.getExpression(DiceRobotChatContent.Expression.WORRY);
					String[] expression = {null, null, Integer.toString(EXPRESSION)};
					expression[0] = tmp[0];
					expression[1] = tmp[1];
					sendChat(expression);
				} else {
					String[] tmp = {null, null};
					tmp = chatContent.getExpression(DiceRobotChatContent.Expression.ANGER);
					String[] expression = {null, null, Integer.toString(EXPRESSION)};
					expression[0] = tmp[0];
					expression[1] = tmp[1];
					sendChat(expression);
				}
			}
			scheduleSendChat(chatFuture , 1);
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

	
	private void scheduleSendOpenDice(final int openType) {
		
		if (openDiceFuture != null){
			openDiceFuture.cancel(false);
		}
		
		openDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendOpenDice(openType);
			}
		}, 
		RandomUtils.nextInt(2)+1, TimeUnit.SECONDS);
	}

	private void sendOpenDice(int openType) {
		ServerLog.info(sessionId, "Robot "+nickName+" open dice");
		
		OpenDiceRequest request = OpenDiceRequest.newBuilder()
				.setOpenType(openType)
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
		
//		// index 0: contentType
//		// index 1: content( only valid for TEXT)
//		// index 2: contentVoiceId or expressionId,depent on contentType
//		final String[] content = diceRobotChatContent.prepareChatContent();
		
		chatFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				if ( RandomUtils.nextInt(2) == 1 ) {
					if ( RandomUtils.nextInt(2) == 1 ) {
						String[] tmp = {null, null};
						tmp = chatContent.getExpression(DiceRobotChatContent.Expression.WORRY);
						String[] expression = {null, null, Integer.toString(EXPRESSION)};
						expression[0] = tmp[0];
						expression[1] = tmp[1];
						sendChat(expression);
					} else {
						String[] tmp = {null, null};
						tmp = chatContent.getExpression(DiceRobotChatContent.Expression.ANGER);
						String[] expression = {null, null, Integer.toString(EXPRESSION)};
						expression[0] = tmp[0];
						expression[1] = tmp[1];
						sendChat(expression);
					}
				}
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
		
		this.robotWinThisGame = robotWinThisGame;
		firstRound = false;
		rollEndChatLock = 2;
	}

}
