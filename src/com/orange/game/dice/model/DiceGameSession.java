package com.orange.game.dice.model;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.security.PropertyUserStore.UserListener;

import com.orange.common.utils.RandomUtil;
import com.orange.game.dice.statemachine.DiceGameStateMachineBuilder;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.network.game.protocol.model.DiceProtos.PBDice;
import com.orange.network.game.protocol.model.DiceProtos.PBUserDice;

public class DiceGameSession extends GameSession {

	
	public static final int DICE_COUNT = 5;		// each user has 5 dice
	
	public static final int DICE_1 = 1;
	public static final int DICE_6 = 6;
	
	ConcurrentHashMap<String, PBUserDice> userDices = new ConcurrentHashMap<String, PBUserDice>();
	
	public DiceGameSession(int sessionId, String name) {
		super(sessionId, name);
		
		// init state
		this.currentState = DiceGameStateMachineBuilder.INIT_STATE;
	}

	public void resetGame(){
		super.resetGame();
		userDices.clear();
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

	
}
