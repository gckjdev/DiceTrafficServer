package com.orange.game.dice.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.security.PropertyUserStore.UserListener;

import com.orange.common.log.ServerLog;
import com.orange.common.utils.RandomUtil;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserResult;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser;

public class DiceGameSession extends GameSession {

	
	public static final int DICE_COUNT = 5;		// each user has 5 dice
	
	public static final int DICE_1 = 1;
	public static final int DICE_6 = 6;
	
	public static final int DICE_OPEN_TYPE_NORMAL = 0;
	public static final int DICE_OPEN_TYPE_QUICK = 1;

	private static final int WIN_COINS = 200;
	
	ConcurrentHashMap<String, PBUserDice> userDices = new ConcurrentHashMap<String, PBUserDice>();
	ConcurrentHashMap<String, PBUserResult> userResults = new ConcurrentHashMap<String, PBUserResult>(); 
	
	volatile int currentDiceNum = -1;
	volatile int currentDice = -1;
	volatile boolean isWilds = false;
	String callDiceUserId;	
	String openDiceUserId;
	volatile int openType = DICE_OPEN_TYPE_NORMAL;
	
	public DiceGameSession(int sessionId, String name) {
		super(sessionId, name);
		
		// init state
		this.currentState = DiceGameStateMachineBuilder.INIT_STATE;
	}

	public void resetGame(){
		super.resetGame();
		userDices.clear();
		userResults.clear();
		clearCallDice();		
		
		isWilds = false;
		openDiceUserId = null;
		openType = DICE_OPEN_TYPE_NORMAL;
	}
	
	private void clearCallDice() {
		callDiceUserId = null;
		currentDice = -1;
		currentDiceNum = -1;
	}

	public void rollDice() {
		
		userDices.clear();
		
		List<GameUser> users = userList.getUserList();
		for (GameUser user : users){
			// random dice for the user and create UserDice
			if (user.isPlaying()){
				PBUserDice userDice = randomRollDice(user.getUserId());
				userDices.put(user.getUserId(), userDice);
			}
		}
		
		ServerLog.info(sessionId, "<rollDice> result="+userDices.toString());
	}

	// roll dice for specific user
	private PBUserDice randomRollDice(String userId) {
		
		PBUserDice.Builder builder = PBUserDice.newBuilder();
		builder.setUserId(userId);
		
		for (int i=0; i<DICE_COUNT; i++){
			int number = RandomUtil.random(DICE_6) + 1;
			
			PBDice dice = PBDice.newBuilder().setDiceId(i+1).setDice(number).build();
			builder.addDices(dice);
		}
		
		return builder.build();
	}

	public Collection<PBUserDice> getUserDices() {
		return userDices.values();
	}

	public void callDice(String userId, int num, int dice) {
		if (userId == null){
			ServerLog.warn(sessionId, "<callDice> but userId is null");
			return;
		}
		
		synchronized (currentPlayUserId) {
			if (!userId.equals(currentPlayUserId)){
				ServerLog.warn(sessionId, "<callDice> but userId "+userId + " is not currentUserId "+currentPlayUserId);
				return;
			}			

			this.callDiceUserId = currentPlayUserId;
		}
		
		this.currentDice = dice;
		this.currentDiceNum = num;
		
		ServerLog.info(sessionId, "<callDice> userId=" +userId + " "+num+" X "+dice);
	}

	public GameResultCode openDice(String userId) {
		
		if (openDiceUserId != null){
			ServerLog.info(sessionId, "<openDice> but open user exists, userId="+openDiceUserId);
			return GameResultCode.ERROR_DICE_ALREADY_OPEN;
		}
		
		ServerLog.info(sessionId, "<openDice> userId="+userId);
		this.openDiceUserId = userId;
		
		if (currentPlayUserId.equals(openDiceUserId)){
			openType = DICE_OPEN_TYPE_NORMAL;
		}
		else {
			openType = DICE_OPEN_TYPE_NORMAL;
		}
		
		return GameResultCode.SUCCESS;
	}

	public boolean canContinueCall() {
		if (currentDice == -1)
			return true;
		
		int playUserCount = getPlayUserCount();		

		// TODO check if can continue call 
		
		return true;
	}

	private void addUserResult(String userId, int gainCoins, boolean isWon){
		PBUserResult result = PBUserResult.newBuilder().
		setWin(isWon).
		setUserId(userId).
		setGainCoins(gainCoins).
		build();
	
		userResults.put(userId, result);		
	}
	
	public void calculateCoins() {
		Collection<PBUserDice> allUserDices = userDices.values();
		
		int resultCount = 0;
		for (PBUserDice userDice : allUserDices){
			List<PBDice> diceList = userDice.getDicesList();
			if (diceList == null)
				continue;
			
			for (PBDice dice : diceList){
				int number = dice.getDice();
				if (number == 1 && isWilds == false){
					resultCount ++;
				}
				else if (number == currentDice){
					resultCount ++;
				}				
			}
		}
		
		ServerLog.info(sessionId, "<result> total "+resultCount + " "+currentDice);
		
		// clear user results
		userResults.clear();
		
		int winCoins = WIN_COINS;
		int lostCoins = -winCoins;
		
		// now check who wins			
		if (resultCount >= currentDiceNum){
			// call dice wins
			addUserResult(callDiceUserId, winCoins, true);
			addUserResult(openDiceUserId, lostCoins, false);
		}
		else{
			// open dice wins
			addUserResult(openDiceUserId, winCoins, true);
			addUserResult(callDiceUserId, lostCoins, false);
		}
	}

	public Collection<PBUserResult> getUserResults(){
		return userResults.values();
	}
	
}
