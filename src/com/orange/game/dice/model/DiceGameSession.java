package com.orange.game.dice.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.math.RandomUtils;

import com.orange.common.log.ServerLog;
import com.orange.common.utils.RandomUtil;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.constants.GameConstantsProtos;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.model.DiceProtos;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceFinalCount;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceType;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserResult;


public class DiceGameSession extends GameSession {

	
	public static final int DICE_COUNT = 5;		// each user has 5 dice
	
	public static final int DICE_1 = 1;
	public static final int DICE_2 = 2;
	public static final int DICE_6 = 6;
	
	public static final int DICE_OPEN_TYPE_NORMAL = 0;
	public static final int DICE_OPEN_TYPE_QUICK = 1;
	public static final int DICE_OPEN_TYPE_CUT = 2;

	private static final int WIN_COINS = 100;
	
	ConcurrentHashMap<String, PBUserDice> userDices = new ConcurrentHashMap<String, PBUserDice>();
	ConcurrentHashMap<String, PBUserResult> userResults = new ConcurrentHashMap<String, PBUserResult>(); 
	
	volatile int currentDiceNum = -1;
	volatile int currentDice = -1;
	volatile boolean isWilds = false;
	String callDiceUserId;	
	String openDiceUserId;
	volatile int openDiceMultiple = 1;	
	volatile int openDiceType = DICE_OPEN_TYPE_NORMAL;
	
<<<<<<< HEAD
	
	public DiceGameSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType) {
		super(sessionId, name, password, createByUser, createBy, ruleType);
=======
	int waitClaimTimeOutTimes = 0;
	
	public DiceGameSession(int sessionId, String name, String password, boolean createByUser, String createBy) {
		super(sessionId, name, password, createByUser, createBy);
>>>>>>> b9ac52ad1f084f716cc35a38a74e6669db3574d1
		
		// init state
		this.currentState = DiceGameStateMachineBuilder.INIT_STATE;
	}

	public void resetGame(){
		super.resetGame();
	}
	
	@Override
	public void restartGame(){	
		clearTimer();
		userDices.clear();
		userResults.clear();
		isWilds = false;
		clearCallDice();				
		clearOpenDice();
	}
	
	private void clearCallDice() {
		callDiceUserId = null;
		currentDice = -1;
		currentDiceNum = -1;
	}
	
	private void clearOpenDice(){
		openDiceUserId = null;
		openDiceType = DICE_OPEN_TYPE_NORMAL;		
		openDiceMultiple = 1;
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
		
		// How six dice face value distributed, initial value is 0 
		int[]  distribution = new int[DICE_6];
		for ( int i = 0 ; i < DICE_6; i++) {
			distribution[i] = 0;
		}
		
		PBDiceType diceType = null;
		
		PBUserDice.Builder builder = PBUserDice.newBuilder().setUserId(userId);
		
		// Roll five dices
		int test = RandomUtils.nextInt(2);
		for (int i = 0; i < DICE_COUNT; i++) {
			int number = 0;
			//For test
			if ( test == 1) {
				number = i+1;// produce snake dices
			}
			else {
				// produce net or wai dices
				number = (RandomUtils.nextInt(2) == 1 ? 1 : 3); 
			}
//			int number = RandomUtil.random(DICE_6) + 1;
			distribution[number-1]++;
			
			PBDice dice = PBDice.newBuilder()
									.setDiceId(i+1)
									.setDice(number)
									.build();
			builder.addDices(dice);
		}
		
		// Decide the diceType
		ServerLog.info(sessionId, "The distribution of dices of user[" + userId + "] is "+ distribution.toString());
		int count = 0; // dice category count
		for ( int i: distribution ) {
			if ( i != 0 ) 
				count++;
		}
		if ( count == 1 ) 
			diceType = PBDiceType.DICE_NET;
		else if ( count == 2 && distribution[DICE_1-1] != 0)
			diceType = PBDiceType.DICE_WAI;
		else if ( count == 5 )
			diceType = PBDiceType.DICE_SNAKE;
		else 
			diceType = PBDiceType.DICE_NORMAL;
		
		
		// Complete the PBUserDice building.
		PBUserDice pbUserDice = builder.setType(diceType).build();
		ServerLog.info(sessionId, "User[" + userId + "]'s PBUserDice is " + pbUserDice.toString());
		
		return pbUserDice;
	}

	public Collection<PBUserDice> getUserDices() {
		return userDices.values();
	}

	public GameResultCode callDice(String userId, int num, int dice, boolean wilds) {
		if (userId == null){
			ServerLog.warn(sessionId, "<callDice> but userId is null");
			return GameResultCode.ERROR_USERID_NULL;
		}
		
		if (this.openDiceUserId != null){
			ServerLog.warn(sessionId, "<callDice> from user"+userId+" but dice has been open, cannot call");
			return GameResultCode.ERROR_DICE_ALREADY_OPEN;			
		}
		
		String currentPlayUserId = getCurrentPlayUserId();		
		synchronized (currentPlayUserId) {
			if (!userId.equals(currentPlayUserId)){
				ServerLog.warn(sessionId, "<callDice> but userId "+userId + " is not currentUserId "+currentPlayUserId);
				return GameResultCode.ERROR_USER_NOT_CURRENT_PLAY_USER;
			}
			else if (callDiceUserId != null && callDiceUserId.equals(userId)){
				ServerLog.warn(sessionId, "<callDice> but userId "+userId + " already call dice");
				return GameResultCode.ERROR_USER_ALREADY_CALL_DICE;				
			}

			this.callDiceUserId = currentPlayUserId;
		}
		
		this.currentDice = dice;
		this.currentDiceNum = num;
		this.isWilds = wilds;
		
		ServerLog.info(sessionId, "<callDice> userId=" +userId + " "+num+" X "+dice + " Wilds="+isWilds);
		return GameResultCode.SUCCESS;
	}

	public GameResultCode openDice(String userId, int openType, int multiple) {
		
		if (openDiceUserId != null){
			ServerLog.info(sessionId, "<openDice> but open user exists, userId="+openDiceUserId);
			return GameResultCode.ERROR_DICE_ALREADY_OPEN;
		}
		
		if (callDiceUserId != null && callDiceUserId.equals(userId)){
			ServerLog.info(sessionId, "<openDice> but you are the call user in last round, userId="+userId);
			return GameResultCode.ERROR_DICE_OPEN_SELF;			
		}
		
		ServerLog.info(sessionId, "<openDice> userId="+userId+", openType="+openType+", multiple="+multiple);
		this.openDiceUserId = userId;
		this.openDiceType = openType;
		this.openDiceMultiple = multiple;
		
		String currentPlayUserId = getCurrentPlayUserId();
		if (currentPlayUserId.equals(openDiceUserId)){
			openDiceType = DICE_OPEN_TYPE_NORMAL;
		}
		else {
			openDiceType = DICE_OPEN_TYPE_NORMAL;
		}
		
		return GameResultCode.SUCCESS;
	}

	public boolean canContinueCall() {
		if (currentDice == -1)
			return true;
		
		int playUserCount = getPlayUserCount();		

		int maxCallCount = playUserCount * 5;
		if (currentDiceNum > maxCallCount){
			return false;
		}
		else if (currentDiceNum == maxCallCount && currentDice == DICE_1){
			return false;
		}
		
		return true;
	}
	
	// whether given userId can open current call dice user Id
	public boolean canOpen(String userId) {

		// TODO Auto-generated method stub
		return true;
	}


	private void addUserResult(String userId, int gainCoins, boolean isWon){
		
		if (userId == null){
			ServerLog.warn(sessionId, "<addUserResult> but userId is null");
			return;
		}
		
		PBUserResult result = PBUserResult.newBuilder().
		setWin(isWon).
		setUserId(userId).
		setGainCoins(gainCoins).
		build();
	
		userResults.put(userId, result);		
	}
	
	
	public List<PBDiceFinalCount> diceCountSettlement(int ruleType) {
		
		Collection<PBUserDice> allUserDices = userDices.values();
		if (allUserDices.size() < 2 || (callDiceUserId == null || openDiceUserId == null)){
			ServerLog.info(sessionId, "<diceCountSettlement> but user dice count="+allUserDices.size()+", callDiceUserId="+callDiceUserId+", openDiceUserId="+openDiceUserId);
			return Collections.emptyList();
		}
		
		List<PBDiceFinalCount> pbDiceFinalCountList = new ArrayList<DiceProtos.PBDiceFinalCount>();
		
		
		// Per user's dices settlement
		for (PBUserDice userDice : allUserDices){
			
			// How six dice's  face value distributed, initial value is 0
			int[] distribution = new int[DICE_6] ;  
	      for(int i = 0 ; i < DICE_6 ; i++) {  
	         distribution[i] = 0 ;  
	         }  
	         
			String userId = userDice.getUserId();
			ServerLog.info(sessionId, "The distribution of dices of user[" + userId + "] is "+ distribution);
			int finalDiceCount = 0;
			PBDiceType diceType = null;
			
			List<PBDice> diceList = userDice.getDicesList();
			if (diceList == null) {
				ServerLog.info(sessionId, "<diceCountSettlement> ERROR:" + userId + "'s diceList is null !!!");
				continue;
			}
			
			// First check how this user's dices distributed.
			for ( PBDice dice : diceList ) {
				int diceValue = dice.getDice();
				distribution[diceValue-1]++;
			}
			int count = 0;
			for ( int i: distribution ) {
				if ( i != 0 ) 
					count++;
			}
			
			// Decide what finalDiceCount is and what diceType is 
			finalDiceCount = distribution[currentDice-1] + (currentDice == DICE_1 ? 0 : distribution[DICE_1-1]*(isWilds ? 0 : 1));
			if ( ruleType == DiceGameRuleType.RULE_NORMAL_VALUE ) {
				diceType = PBDiceType.DICE_NORMAL;
			}
			else if ( ruleType == DiceGameRuleType.RULE_HIGH_VALUE || ruleType == DiceGameRuleType.RULE_SUPER_HIGH_VALUE) {
				if ( count == 1 && finalDiceCount == 5 ) {
					if (distribution[DICE_1-1] == 5 && currentDice != DICE_1) {
						finalDiceCount = 6;
						diceType = PBDiceType.DICE_WAI;
					} else {
						finalDiceCount = 7;
						diceType = PBDiceType.DICE_NET;
					}
				}
				else if ( count == 2 && finalDiceCount == 5 ) {
					finalDiceCount = 6;
					diceType = PBDiceType.DICE_WAI;
				}
				else if ( count == 5 ) {
					finalDiceCount = 0;
					diceType = PBDiceType.DICE_SNAKE;
				}
				else  {
					diceType = PBDiceType.DICE_NORMAL;
				}
			}
				
			// Build the PBDiceFinalCount 
			PBDiceFinalCount diceDiceFinalCount = PBDiceFinalCount.newBuilder()
					.setUserId(userId)
					.setType(diceType)
					.setFinalDiceCount(finalDiceCount)
					.build();
			
			pbDiceFinalCountList.add(diceDiceFinalCount);
		}
		
		// clear user results
		userResults.clear();
		
		return pbDiceFinalCountList;
	}

	public void calculateCoins(int allFinalCount) {
		
		int times = (allFinalCount > currentDiceNum ?  allFinalCount - currentDiceNum : currentDiceNum - allFinalCount) + 1;
		int winCoins = WIN_COINS *  times * this.openDiceMultiple;
		int lostCoins = -winCoins;

		ServerLog.info(sessionId, "<diceCountSettlement> result = total "+allFinalCount + " X "+currentDice);
		
		// now check who wins			
		if (allFinalCount >= currentDiceNum){
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

	public void autoCallOrOpen() {
	}
	
	public boolean isOpen(){
		return (openDiceUserId != null);
	}

	public String getCallDiceUserId() {
		return callDiceUserId;
	}

	public int getCurrentDiceNum() {
		return currentDiceNum;
	}

	public void setCurrentDiceNum(int currentDiceNum) {
		this.currentDiceNum = currentDiceNum;
	}

	public int getCurrentDice() {
		return currentDice;
	}

	public void setCurrentDice(int currentDice) {
		this.currentDice = currentDice;
	}

	public String getLoserUserId(){
		for (PBUserResult result : userResults.values()){
			if (result.getWin() == false){
				return result.getUserId();
			}
		}
		
		return null;
	}

	public boolean reachMaxDice(int diceNum) {
		int playUserCount = getPlayUserCount();		

		int maxCallCount = playUserCount * 5;
		if (diceNum >= maxCallCount){
			return true;
		}
		
		return false;
	}

	public void setCurrentPlayUser(int index) {
		userList.selectCurrentPlayUser(index);
	}

	public boolean getIsWilds() {
		return isWilds;
	}

	public PBUserDice rollDiceAgain(String userId) {		
		PBUserDice userDice = randomRollDice(userId);
		userDices.put(userId, userDice);
		return userDice;
	}

	public void incWaitClaimTimeOutTimes() {
		waitClaimTimeOutTimes ++;
	}

	public void clearWaitClaimTimeOutTimes() {
		waitClaimTimeOutTimes = 0;
	}



	
}
