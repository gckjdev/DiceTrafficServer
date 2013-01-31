package com.orange.game.dice.statemachine;

import com.orange.common.statemachine.Action;
import com.orange.common.statemachine.DecisionPoint;
import com.orange.common.statemachine.State;
import com.orange.common.statemachine.StateMachine;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.dice.statemachine.state.GameState;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.statemachine.CommonGameAction;
import com.orange.game.traffic.statemachine.CommonGameState;
import com.orange.game.traffic.statemachine.CommonStateMachineBuilder;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;

public class DiceGameStateMachineBuilder extends CommonStateMachineBuilder {

	// thread-safe singleton implementation
    private static DiceGameStateMachineBuilder builder = new DiceGameStateMachineBuilder();
    public static final State INIT_STATE = new CommonGameState(GameStateKey.CREATE);
    
    private DiceGameStateMachineBuilder(){		
	} 	
    public static DiceGameStateMachineBuilder getInstance() {         	
    	return builder; 
    } 
    	
    public static final int START_GAME_TIMEOUT = 3;			// 36 seconds, 20 for start, 10 for result, 6 for reserved
	public static final int WAIT_CLAIM_TIMEOUT = 18;
	public static final int ROLL_DICE_TIMEOUT = 3;
	public static final int SHOW_RESULT_TIMEOUT = 10;
	public static final int TAKEN_OVER_USER_WAIT_TIMEOUT = 1;
	public static final int WAIT_USER_BET_TIMEOUT = 7;
    	
	final Action completeGame = new DiceGameAction.CompleteGame();
	final Action rollDiceAndBroadcast = new DiceGameAction.RollDiceAndBroadcast();
	final Action broadcastRollDiceBegin = new DiceGameAction.BroadcastRollDiceBegin();
	final Action broadcastNextPlayerNotification = new DiceGameAction.BroadcastNextPlayerNotification();
	final Action directOpenDice = new DiceGameAction.DirectOpenDice();
	final Action autoCallOrOpen = new DiceGameAction.AutoCallOrOpen();

	// timers
	final Action setStartGameTimer = new CommonGameAction.CommonTimer(START_GAME_TIMEOUT, DiceGameAction.DiceTimerType.START);
	final Action setRollDiceBeginTimer = new CommonGameAction.CommonTimer(ROLL_DICE_TIMEOUT, DiceGameAction.DiceTimerType.ROLL_DICE);
	final Action setWaitClaimTimer = new DiceGameAction.SetWaitClaimTimer(WAIT_CLAIM_TIMEOUT, DiceGameAction.DiceTimerType.WAIT_CLAIM);
	final Action setTakenOverUserWaitTimer = new CommonGameAction.CommonTimer(TAKEN_OVER_USER_WAIT_TIMEOUT, DiceGameAction.DiceTimerType.TAKEN_OVER_USER_WAIT);
	final Action setShowResultTimer = new DiceGameAction.SetShowResultTimer();		
	final Action setWaitBetTimer = new CommonGameAction.CommonTimer(WAIT_USER_BET_TIMEOUT, DiceGameAction.DiceTimerType.WAIT_USER_BET);

	final Action clearWaitClaimTimeOutTimes = new DiceGameAction.ClearWaitClaimTimeOutTimes();
	final Action kickWaitTimeOutUsers = new DiceGameAction.KickWaitTimeOutUsers();
	
	final Action restartGame = new DiceGameAction.RestartGame();
	final Action selectLoserAsCurrentPlayerUser = new DiceGameAction.SelectLoserAsCurrentPlayerUser();
	final Action kickTakenOverUser = new DiceGameAction.KickTakenOverUser();
	
	final Action queryUserBalance = new CommonGameAction.QueryUserBalance();
	
    @Override
	public StateMachine buildStateMachine() {
    	
		StateMachine sm = new StateMachine();
		
		sm.addState(INIT_STATE)		
			.addAction(initGame)
			.addAction(clearTimer)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_TIME_OUT)			
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addAction(selectPlayUser);
		
		sm.addState(new GameState(GameStateKey.CHECK_USER_COUNT))
			.setDecisionPoint(new DecisionPoint(checkUserCount){
				@Override
				public Object decideNextState(Object context){
					GameSession session = (GameSession)context;
					int userCount = condition.decide(context);
					if (userCount == 0){
						if ( session.isCreatedByUser() ) {
							GameEventExecutor.getInstance().executeForSessionRealease(session.getSessionId());
							return null;
						}
						return GameStateKey.CREATE;
					}
					else if (userCount == 1){ // only one user
						return GameStateKey.ONE_USER_WAITING;
					}
					else{ // more than one user
						return GameStateKey.WAIT_FOR_START_GAME;
					}
				}
			});
		
		sm.addState(new GameState(GameStateKey.ONE_USER_WAITING))
			.addAction(setOneUserWaitTimer)
			.addAction(prepareRobot)
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CREATE)	
			.addTransition(GameCommandType.LOCAL_OTHER_USER_QUIT, GameStateKey.CREATE)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_PLAY_USER)	
			.addAction(clearTimer)
			.addAction(clearRobotTimer);				
		
		sm.addState(new GameState(GameStateKey.WAIT_FOR_START_GAME))
			.addAction(setStartGameTimer)
			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.PLAY_USER_QUIT)
			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.ROLL_DICE_BEGIN)				
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.PLAY_USER_QUIT))
			.addAction(kickPlayUser)
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});				
		
		sm.addState(new GameState(GameStateKey.KICK_PLAY_USER))
			.addAction(kickPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
				}
			});
		
		sm.addState(new GameState(GameStateKey.ROLL_DICE_BEGIN))
			.addAction(startGame)
			.addAction(queryUserBalance)
			.addAction(broadcastRollDiceBegin)
			.addAction(setRollDiceBeginTimer)
//			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.PLAY_USER_QUIT)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.CHECK_NEXT_PLAYER_PLAY)				
			.addAction(clearTimer)
			.addAction(rollDiceAndBroadcast);
		
		sm.addState(new GameState(GameStateKey.CHECK_NEXT_PLAYER_PLAY))
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session = (DiceGameSession)context;
					GameUser user = session.getCurrentPlayUser();
					if (session.getUserCount() <= 1){
						return GameStateKey.CHECK_USER_COUNT;
					}
					
					if (user == null || user.isTakenOver() == false){
						return GameStateKey.WAIT_NEXT_PLAYER_PLAY;
					}
					else{
						return GameStateKey.TAKEN_OVER_USER_WAIT;
					}
				}
			});				

		sm.addState(new GameState(GameStateKey.TAKEN_OVER_USER_WAIT))
			.addAction(broadcastNextPlayerNotification)
			.addAction(setTakenOverUserWaitTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.AUTO_CALL_OR_OPEN_DICE)
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.WAIT_NEXT_PLAYER_PLAY))
			.addAction(broadcastNextPlayerNotification)
			.addAction(setWaitClaimTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_CALL_DICE, GameStateKey.PLAYER_CALL_DICE)		
			.addTransition(GameCommandType.LOCAL_OPEN_DICE, GameStateKey.CHECK_OPEN_DICE)		
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.AUTO_CALL_OR_OPEN_DICE)
			.addTransition(GameCommandType.LOCAL_USER_SKIP, GameStateKey.SKIP_USER)
			.addAction(clearTimer);		
		
		sm.addState(new GameState(GameStateKey.SKIP_USER))
			.addAction(clearWaitClaimTimeOutTimes)				
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
						return GameStateKey.CHECK_NEXT_PLAYER_PLAY;						
				}
		});			
		

		sm.addState(new GameState(GameStateKey.PLAYER_CALL_DICE))
			.addAction(clearWaitClaimTimeOutTimes)		
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session = (DiceGameSession)context;
					if (session.canContinueCall()){
						return GameStateKey.CHECK_NEXT_PLAYER_PLAY;						
					}
					else{
						return GameStateKey.DIRECT_OPEN_DICE;
					}
				}
			});			
		
		sm.addState(new GameState(GameStateKey.AUTO_CALL_OR_OPEN_DICE))
			.addAction(autoCallOrOpen)
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session = (DiceGameSession)context;
					int playUserCount = session.getPlayUserCount();
					if (session.isAllUserTakenOver()){
						if (playUserCount == 2 ) {
							return GameStateKey.COMPLETE_GAME;
						} 
						return GameStateKey.WAIT_BET;
					}
					else if (session.isOpen()){
						if (playUserCount == 2) {
							return GameStateKey.COMPLETE_GAME;
						}
						return GameStateKey.WAIT_BET;
					}
					else{
						return GameStateKey.CHECK_NEXT_PLAYER_PLAY;	// goto check user count state directly
					}
				}
			});	
		
		sm.addState(new GameState(GameStateKey.DIRECT_OPEN_DICE))
			.addAction(directOpenDice)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session =(DiceGameSession)context;
					int playUserCount = session.getPlayUserCount();
					if (playUserCount == 2) {
						return GameStateKey.COMPLETE_GAME;	
					}
					return GameStateKey.WAIT_BET;
				}
			});			

		
		sm.addState(new GameState(GameStateKey.CHECK_OPEN_DICE))
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session =(DiceGameSession)context;
					int playUserCount = session.getPlayUserCount();
					if (playUserCount == 2) {
						return GameStateKey.COMPLETE_GAME;	
					}
					return GameStateKey.WAIT_BET;
				}
			});			
		
		sm.addState(new GameState(GameStateKey.WAIT_BET))
			.addAction(setWaitBetTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)			
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.COMPLETE_GAME)
			.addTransition(GameCommandType.LOCAL_ALL_USER_BET, GameStateKey.COMPLETE_GAME)
			.addAction(clearTimer);
		
		
		sm.addState(new GameState(GameStateKey.COMPLETE_GAME))
			.addAction(completeGame)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){					
					return GameStateKey.SHOW_RESULT;	// goto check user count state directly
				}
			});
		
		
		sm.addState(new GameState(GameStateKey.SHOW_RESULT))
			.addAction(setShowResultTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)			
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.CHECK_USER_COUNT)
			.addAction(kickTakenOverUser)
			.addAction(clearAllUserPlaying)
			.addAction(kickWaitTimeOutUsers)
			.addAction(selectLoserAsCurrentPlayerUser)
			.addAction(restartGame);
		
		sm.printStateMachine();		
		return sm;
	}

}
