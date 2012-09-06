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
	
	public DiceGameSession(int sessionId, String name, String password, boolean createByUser, String createBy) {
		super(sessionId, name, password, createByUser, createBy);
		
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
	
	public void calculateCoins() {
		Collection<PBUserDice> allUserDices = userDices.values();
		if (allUserDices.size() < 2 || (callDiceUserId == null || openDiceUserId == null)){
			ServerLog.info(sessionId, "<calculateCoins> but user dice count="+allUserDices.size()+" callDiceUserId="+callDiceUserId+", openDiceUserId"+openDiceUserId);
			return;
		}
		
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
		
		ServerLog.info(sessionId, "<calculateCoins> result = total "+resultCount + " X "+currentDice);
		
		// clear user results
		userResults.clear();
		
		int winCoins = WIN_COINS * this.openDiceMultiple;
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
		
		ServerLog.info(sessionId, "<calculateCoins> user result="+userResults.toString());
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



	
}
