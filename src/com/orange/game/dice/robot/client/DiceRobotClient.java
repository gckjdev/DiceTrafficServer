package com.orange.game.dice.robot.client;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.RandomUtils;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.robot.client.AbstractRobotClient;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.CallDiceRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.OpenDiceRequest;

public class DiceRobotClient extends AbstractRobotClient {

	String openUserId = null;
	String callUserId = null;
	int callDice = -1;
	int callDiceNum = -1;
	boolean callDiceIsWild = false;
	
	ScheduledFuture<?> callDiceFuture = null;
	
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
			// TODO record dice information for later call/open
			ServerLog.info(sessionId, "Robot "+nickName+" receive ROLL_DICE_END_NOTIFICATION_REQUEST");
			break;
			
		case NEXT_PLAYER_START_NOTIFICATION_REQUEST:
			ServerLog.info(sessionId, "Robot "+nickName+" receive NEXT_PLAYER_START_NOTIFICATION_REQUEST");
			if (message.getCurrentPlayUserId().equals(userId)){
				// you turn... send call dice request or open dice request
				if (this.sessionRealUserCount() == 0){
					// open dice
					sendOpenDice();
				}				
				else if (canOpenDice() && RandomUtils.nextInt() % 2 == 0){
					sendOpenDice();
				}
				else if (canCallDice()){
					scheduleSendCallDice();
				}
			}
			break;
			
		case CALL_DICE_REQUEST:
			callUserId = message.getUserId();
			callDice = message.getCallDiceRequest().getDice();
			callDiceNum = message.getCallDiceRequest().getNum();
			if (message.getCallDiceRequest().hasWilds()){
				callDiceIsWild = message.getCallDiceRequest().getWilds();
			}
			ServerLog.info(sessionId, "Robot "+nickName+" receive CALL_DICE_REQUEST");
			break;
			
		case OPEN_DICE_REQUEST:
			openUserId = message.getUserId();
			ServerLog.info(sessionId, "Robot "+nickName+" receive OPEN_DICE_REQUEST");
			break;
		}
	}

	private void scheduleSendCallDice() {
		if (callDiceFuture != null){
			callDiceFuture.cancel(false);
		}
		
		callDiceFuture = scheduleService.schedule(new Runnable() {			
			@Override
			public void run() {
				sendCallDice();
			}
		}, 
		RandomUtils.nextInt(5)+1, TimeUnit.SECONDS);
	}

	private void sendCallDice() {
		// TODO Auto-generated method stub
		int dice = DiceGameSession.DICE_2;
		int diceNum = userList.size()+1;
		boolean isWild = callDiceIsWild;
		
		if (callDice == -1){
			// no one call before			
		}
		else if (callDice == DiceGameSession.DICE_6){
			dice = DiceGameSession.DICE_1;
			diceNum = callDiceNum;
			isWild = true;
		}
		else{
			dice = callDice;
			diceNum = callDiceNum + 1;
		}														
		
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

	private boolean canOpenDice() {
		return (callUserId != null);
	}
	
	private boolean canCallDice(){
		return (openUserId == null);
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
	public void resetPlayData() {
		openUserId = null;
		callUserId = null;
		callDice = -1;
		callDiceNum = -1;
		callDiceIsWild = false;		
	}

}
