package com.orange.game.dice.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.math.RandomUtils;
import com.orange.common.log.ServerLog;
import com.orange.common.mongodb.MongoDBClient;
import com.orange.common.utils.RandomUtil;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.model.dao.User;
import com.orange.game.model.manager.UserManager;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.BetDiceRequest;
import com.orange.network.game.protocol.model.DiceProtos;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceFinalCount;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceType;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;


public class DiceGameSession extends GameSession {

	
	public static final int DICE_COUNT = 5;		// each user has 5 dice
	
	public static final int DICE_1 = 1;
	public static final int DICE_2 = 2;
	public static final int DICE_6 = 6;
	
	public static final int DICE_OPEN_TYPE_NORMAL = 0;
	public static final int DICE_OPEN_TYPE_QUICK = 1;
	public static final int DICE_OPEN_TYPE_CUT = 2;

	private static final int WIN_COINS = 100;

	private static final int MAX_AUTO_TIME_OUT = 15;
	
	ConcurrentHashMap<String, PBUserDice> userDices = new ConcurrentHashMap<String, PBUserDice>();
	
	ConcurrentHashMap<String, Integer> userAutoCallTimesMap = new ConcurrentHashMap<String, Integer>();
	ConcurrentHashMap<String, GameMessageProtos.BetDiceRequest> userBetMap = new ConcurrentHashMap<String, GameMessageProtos.BetDiceRequest>();

	
	
	volatile int currentDiceNum = -1;
	volatile int currentDice = -1;
	volatile boolean isWilds = false;
	// how many rounds this game has go for?
	private int playRound = 0 ;
	
	// how many players have bet?
	volatile int userBetCount = 0;
	

	String callDiceUserId;	
	String openDiceUserId;
	String loserUserId = null;
	volatile int openDiceMultiple = 1;	
	volatile int openDiceType = DICE_OPEN_TYPE_NORMAL;

	// Does next player's timer get decreased? 
	private boolean decreaseTimeForNextPlayUser = false;


	
	public DiceGameSession(int sessionId, String name, String password, boolean createByUser, String createBy, int ruleType,int testEnable) {
		super(sessionId, name, password, createByUser, createBy, ruleType, testEnable);
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
		userBetMap.clear();
		clearCallDice();				
		clearOpenDice();
		isWilds = false;
		userBetCount = 0;
		decreaseTimeForNextPlayUser = false;
	}
	
	private void clearCallDice() {
		callDiceUserId = null;
		currentDice = -1;
		currentDiceNum = -1;
		playRound = 0;
	}
	
	private void clearOpenDice(){
		openDiceUserId = null;
		openDiceType = DICE_OPEN_TYPE_NORMAL;		
		openDiceMultiple = 1;
	}

	public void rollDice() {
		
		userDices.clear();
		
		List<GameUser> users = gameSessionUserList.getAllUsers();
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
			if ( testEnable == 1) {
				//For test
				if ( test == 1) {
					number = i+1;// produce snake dices
				}
				else {
					// produce net or wai dices
					int dice = RandomUtil.random(DICE_6);
					number = (RandomUtils.nextInt(2) == 1 ? 1 : dice); 
				}
			} else {
				number = RandomUtil.random(DICE_6) + 1;
			}
			distribution[number-1]++;
			
			PBDice dice = PBDice.newBuilder()
									.setDiceId(i+1)
									.setDice(number)
									.build();
			builder.addDices(dice);
		}
		
		// Decide the diceType
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
		ServerLog.info(sessionId, "<DiceGameSession>User[" + userId + "]'s PBUserDice is " + pbUserDice.toString());
		
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
		if (currentPlayUserId == null){
			ServerLog.warn(sessionId, "<callDice> but current play user is null?");
			return GameResultCode.ERROR_CURRENT_PLAY_USER_NULL;
		}
		
		synchronized (currentPlayUserId) {
			ServerLog.info(sessionId, "callDiceUserId="+callDiceUserId+",userId="+userId);
			if (!userId.equals(currentPlayUserId)){
				ServerLog.warn(sessionId, "<callDice> but userId "+userId + " is not currentUserId "+currentPlayUserId);
				return GameResultCode.ERROR_USER_NOT_CURRENT_PLAY_USER;
			}
			// why should check directionChanged is explained in the ReverCallItemHandler.
			else if (callDiceUserId != null && directionChanged == false &&callDiceUserId.equals(userId) ){
				ServerLog.warn(sessionId, "<callDice> but userId "+userId + " already call dice");
				return GameResultCode.ERROR_USER_ALREADY_CALL_DICE;				
			}
//			this.callDiceUserId = currentPlayUserId;
			setCallDiceUserId(currentPlayUserId);
		}
		
		this.currentDice = dice;
		this.currentDiceNum = num;
		this.isWilds = wilds;
		this.playRound++;
		
		ServerLog.info(sessionId, "<callDice> userId=" +userId + " "+num+" X "+dice + ", Wilds="+isWilds+ ", playRound="+playRound);
		return GameResultCode.SUCCESS;
	}

	public GameResultCode openDice(String userId, int openType, int multiple) {
		
		if (openDiceUserId != null){
			ServerLog.info(sessionId, "<openDice> but open user exists, userId="+openDiceUserId);
			return GameResultCode.ERROR_DICE_ALREADY_OPEN;
		}
		
		if (callDiceUserId == null){
			ServerLog.info(sessionId, "<openDice> but callDiceUserId not exists");
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
			openDiceType = DICE_OPEN_TYPE_QUICK;
		}
		
		return GameResultCode.SUCCESS;
	}

	public boolean canContinueCall() {
		if (currentDice == -1)
			return true;
		
		int maxCallCount = getDiceCallCeiling();
		
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
			int[] distribution = new int[DiceGameConstant.DICE_NUM] ;  
	      for(int i = 0 ; i < DiceGameConstant.DICE_NUM ; i++) {  
	         distribution[i] = 0 ;  
	         }  
	         
			String userId = userDice.getUserId();
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
			if ( currentDice < DICE_1 || currentDice > DICE_6 ) {
				return Collections.emptyList();
			}
			finalDiceCount = distribution[currentDice-1] + (currentDice == DICE_1 ? 0 : distribution[DICE_1-1]*(isWilds ? 0 : 1));
			if ( ruleType == DiceGameRuleType.RULE_NORMAL_VALUE ) {
				diceType = PBDiceType.DICE_NORMAL;
			}
			else if ( ruleType == DiceGameRuleType.RULE_HIGH_VALUE || ruleType == DiceGameRuleType.RULE_SUPER_HIGH_VALUE) {
				if ( count == 1 && finalDiceCount == 5 ) {
					if (distribution[DICE_1-1] == 5 && currentDice != DICE_1) {
						finalDiceCount = DiceGameConstant.DICE_WAI_FINAL_COUNT;
						diceType = PBDiceType.DICE_WAI;
					} else {
						finalDiceCount = DiceGameConstant.DICE_NET_FINAL_COUNT;
						diceType = PBDiceType.DICE_NET;
					}
				}
				else if ( count == 2 && finalDiceCount == 5 ) {
					finalDiceCount = DiceGameConstant.DICE_WAI_FINAL_COUNT;
					diceType = PBDiceType.DICE_WAI;
				}
				else if ( count == 5 ) {
					finalDiceCount = DiceGameConstant.DICE_SNAKE_FINAL_COUNT;
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

	public void calculateCoins(int allFinalCount, int ruleType) {
		
		
		int times = (ruleType == DiceGameRuleType.RULE_NORMAL_VALUE? 
				1 :(allFinalCount > currentDiceNum ?  allFinalCount - currentDiceNum : currentDiceNum - allFinalCount) + 1);
		int ante = 50 + playRound * 50;
		
		// TODO: should move this code to another place, db operation may block the thread running
		MongoDBClient mongoDBClient = getDBInstance();
		User callUser = UserManager.findUserAccountInfoByUserId(mongoDBClient, callDiceUserId);
		User openUser = UserManager.findUserAccountInfoByUserId(mongoDBClient, openDiceUserId);
		
		if (callUser == null || openUser == null){
			ServerLog.warn(sessionId, "<calculateCoins> but callUserId "+callDiceUserId+
					" or openUserId "+openDiceUserId+" not found");
		}		
		
		int winCoins = ( ruleType == DiceGameRuleType.RULE_SUPER_HIGH_VALUE? ante: WIN_COINS)* times * this.openDiceMultiple;
		int lostCoins = -winCoins;

		ServerLog.info(sessionId, "<diceCountSettlement> result = total "+allFinalCount + " X "+currentDice);
		
		// now check who wins			
		if (allFinalCount >= currentDiceNum){
			// call-dice user wins
			int balance = (openUser != null) ? openUser.getBalance() : Integer.MAX_VALUE;
			if ( balance > 0 && balance < -lostCoins )  {
				lostCoins = -balance;
				winCoins = -lostCoins;
			}
			addUserResult(callDiceUserId, winCoins, true);
			addUserResult(openDiceUserId, lostCoins, false);	
			loserUserId = openDiceUserId;
		}
		else{
			// open-dice user wins
			int balance = (callUser != null) ? callUser.getBalance() : Integer.MAX_VALUE;
			if ( balance >0 && balance < -lostCoins ) {
				lostCoins = -balance;
				winCoins = -lostCoins;
			}
			addUserResult(openDiceUserId, winCoins, true);
			addUserResult(callDiceUserId, lostCoins, false);
			loserUserId = callDiceUserId;
		}
		
		// for the gamblers
		for ( Map.Entry<String, BetDiceRequest> entry : userBetMap.entrySet()) {
			String userId = entry.getKey();
			BetDiceRequest request = entry.getValue();
			int gainCoins = (int)(request.getOdds()*request.getAnte());
			// open-dice user loses
			if ( allFinalCount >= currentDiceNum) {
				// bet the open-dice user loses
				if ( request.getOption() == 1) {
					addUserResult(userId, gainCoins, true);
				}
				// bet the open-dice user wins
				else {
					addUserResult(userId, -request.getAnte(), false);
				}
			}
			// open-dice user wins
			else {
				// bet the open-dice user wins
				if ( request.getOption() == 0 ) {
					addUserResult(userId, gainCoins, true);
				} 
				// bet the open-dice user loses
				else {
					addUserResult(userId, -request.getAnte(), false);
				}
			}
		}// end for
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
		return loserUserId;
	}

	public boolean reachMaxDice(int diceNum) {
		
		int maxCallCount = getDiceCallCeiling();
		
		if (diceNum >= maxCallCount){
			return true;
		}
		
		return false;
	}

	public void setCurrentPlayUser(int index) {
		gameSessionUserList.selectCurrentPlayUser(index);
	}

	public boolean getIsWilds() {
		return isWilds;
	}

	public PBUserDice rollDiceAgain(String userId) {		
		PBUserDice userDice = randomRollDice(userId);
		userDices.put(userId, userDice);
		return userDice;
	}

	public void incWaitClaimTimeOutTimes(String userId) {
		if (userId == null)
			return;
		
		Integer times = userAutoCallTimesMap.get(userId);
		if (times == null){
			times = Integer.valueOf(1);
		}
		else{
			times = Integer.valueOf(times.intValue()+1);
		}
		userAutoCallTimesMap.put(userId, times);
		ServerLog.info(sessionId, "Update userId "+userId+" timeout times to "+times.intValue());
	}

	public void clearWaitClaimTimeOutTimes(String userId) {
		if (userId == null)
			return;

		userAutoCallTimesMap.remove(userId);
		ServerLog.info(sessionId, "Clear userId "+userId+" timeout times");
	}

	public void clearAllUserTimeOutTimes(){
		userAutoCallTimesMap.clear();
	}

	public List<String> getWaitTimeOutUsers() {
		Set<Entry<String, Integer>> set =  userAutoCallTimesMap.entrySet();
		List<String> retList = new ArrayList<String>();		
		for (Entry<String, Integer> obj : set){
			String userId = obj.getKey();
			if (obj.getValue().intValue() > MAX_AUTO_TIME_OUT && gameSessionUserList.getUser(userId) != null){
				retList.add(userId);
			}
		}
		return retList;
	}

	public void recordUserBet(String userId, BetDiceRequest betDiceRequest) {
		if ( userBetMap.contains(userId) ) 
			return;
		else 
			userBetMap.put(userId, betDiceRequest);
		
	}



	public void setCallDiceUserId(String userId) {
		callDiceUserId = userId;
	}
	

	public int getUserBetCount() {
		return userBetCount;
	}

	synchronized public void incUserBetCount() {
		userBetCount++;
	}


	public void setDecreaseTimeForNextPlayUser(boolean b) {
		decreaseTimeForNextPlayUser  = b;		
	}


	public boolean getDecreaseTimeForNextPlayUser() {
		return decreaseTimeForNextPlayUser;
	}
	
	private int getDiceCallCeiling() {
		
		int ruleType = getRuleType();
		int playUserCount = getPlayUserCount();
		
		return playUserCount * (ruleType == DiceGameRuleType.RULE_NORMAL_VALUE ? 5 :7);
		
	}
}
