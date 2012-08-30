package com.orange.game.dice.robot.client;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.math.RandomUtils;
import com.orange.common.log.ServerLog;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
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

	
	
	int[] robotRollResult={0,0,0,0,0,0};
	
	List<PBUserDice> pbUserDiceList = null;
	List<PBDice> pbDiceList = null;
	
	ScheduledFuture<?> callDiceFuture = null;
	
	
	
	DiceRobotIntelligence diceRobotIntelligence = new DiceRobotIntelligence(playerCount);
	
	public DiceRobotClient(String userId, String nickName, String avatar,
			boolean gender, String location, int sessionId, int index) {
		super(userId, nickName, avatar, gender, location, sessionId, index);
	}
	
	@Override
	public void handleMessage(GameMessage message){
		switch (message.getCommand()){
		
		case ROLL_DICE_BEGIN_NOTIFICATION_REQUEST:
			ServerLog.info(sessionId, "Robot "+nickName+" receive ROLL_DICE_BEGIN_NOTIFICATION_REQUEST");
			break;
		
		case ROLL_DICE_END_NOTIFICATION_REQUEST:			
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
			diceRobotIntelligence.inspectRobotDices(robotRollResult);
			ServerLog.info(sessionId, "Robot "+nickName+" receive ROLL_DICE_END_NOTIFICATION_REQUEST");
			break;
			
		case NEXT_PLAYER_START_NOTIFICATION_REQUEST:
			ServerLog.info(sessionId, "Robot "+nickName+" receive NEXT_PLAYER_START_NOTIFICATION_REQUEST");
			if (message.getCurrentPlayUserId().equals(userId)){
				
				if ( this.sessionRealUserCount() == 0 || canOpenDice ){
					ServerLog.info(sessionId, "[NEXT_PLAYER_START_NOTIFICATION_REQUEST] robotRollResult dicides to open.");
					sendOpenDice();
				}
				else {
					// Make a decision what to call.
					diceRobotIntelligence.decideWhatToCall(playerCount, callDiceNum, callDice, callDiceIsWild, robotRollResult);
					// Check the decision.
					if (diceRobotIntelligence.giveUpCall()) {
						ServerLog.info(sessionId, "[NEXT_PLAYER_START_NOTIFICATION_REQUEST] robot gives up call ,just open.");
						sendOpenDice();
					} else {
						scheduleSendCallDice(diceRobotIntelligence.getWhatTocall());
					}
				}
			}
			break;
			
		case CALL_DICE_REQUEST:
			callUserId = message.getUserId();
			callDice = message.getCallDiceRequest().getDice();
			callDiceNum = message.getCallDiceRequest().getNum();
			if (message.getCallDiceRequest().hasWilds()){
				callDiceIsWild = message.getCallDiceRequest().getWilds();
			} else {
				callDiceIsWild = false;
			}
			// Get the callUser's seatId
			callUserSeatId = userList.get(callUserId).getSeatId();
			playerCount = userList.size();
			ServerLog.info(sessionId, "Robot " + nickName + " receive CALL_DICE_REQUEST");
			ServerLog.info(sessionId, "The playerCount is " + playerCount);
			if (diceRobotIntelligence.canOpenDice(playerCount,callUserSeatId , callDiceNum, callDice, callDiceIsWild)) {
				ServerLog.info(sessionId, "Robot " + nickName + " decide to open " + callUserId);
//				// Next player is not robot.
//				if ( (callUserSeatId + 1) % playerCount != userList.get(userId).getSeatId() ) {
//					ServerLog.info(sessionId, "[CALL_DICE_RUQUET] sendOpenDice()");
//					sendOpenDice();
//				}
//				else { 
					canOpenDice = true;
//				}
			}
			break;
			
		case OPEN_DICE_REQUEST:
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


	private void sendOpenDice() {
		ServerLog.info(sessionId, "Robot "+nickName+" open dice");
		
		OpenDiceRequest request = OpenDiceRequest.newBuilder().build();
		GameMessage message = GameMessage.newBuilder()
			.setOpenDiceRequest(request)
			.setMessageId(getClientIndex())
			.setCommand(GameCommandType.OPEN_DICE_REQUEST)
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);		
	}

	@Override
	public void resetPlayData(boolean robotWinThisRound) {
		openUserId = null;
		callUserId = null;
		callDice = -1;
		callDiceNum = -1;
		callDiceIsWild = false;
		callUserSeatId = -1;
		canOpenDice = false;
		pbUserDiceList = null;
		pbDiceList = null;
		
		diceRobotIntelligence.balanceAndReset(robotWinThisRound);
	}
	
	public void balanceRobot(boolean robotWinThisRound) {
		
	}

}
