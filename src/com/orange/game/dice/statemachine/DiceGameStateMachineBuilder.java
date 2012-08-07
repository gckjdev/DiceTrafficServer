package com.orange.game.dice.statemachine;

import com.orange.common.statemachine.Action;
import com.orange.common.statemachine.Condition;
import com.orange.common.statemachine.DecisionPoint;
import com.orange.common.statemachine.State;
import com.orange.common.statemachine.StateMachine;
import com.orange.common.statemachine.StateMachineBuilder;
import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.dice.statemachine.action.DiceGameAction;
import com.orange.game.dice.statemachine.action.GameCondition;
import com.orange.game.dice.statemachine.state.GameState;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.game.traffic.model.manager.GameSessionAllocationManager;
import com.orange.game.traffic.statemachine.CommonGameAction;
import com.orange.game.traffic.statemachine.CommonGameCondition;
import com.orange.game.traffic.statemachine.CommonGameState;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.RollDiceBeginNotificationRequest;

public class DiceGameStateMachineBuilder extends StateMachineBuilder {

	// thread-safe singleton implementation
    private static DiceGameStateMachineBuilder builder = new DiceGameStateMachineBuilder();
    public static final State INIT_STATE = new CommonGameState(GameStateKey.CREATE);
    
    private DiceGameStateMachineBuilder(){		
	} 	
    public static DiceGameStateMachineBuilder getInstance() {         	
    	return builder; 
    } 
    
	enum DiceTimerType{
		START, ROLL_DICE, WAIT_CLAIM, SHOW_RESULT
	};
	
	static final int START_GAME_TIMEOUT = 3;			// 36 seconds, 20 for start, 10 for result, 6 for reserved
	static final int WAIT_CLAIM_TIMEOUT = 17;
	static final int ROLL_DICE_TIMEOUT = 3;
	static final int SHOW_RESULT_TIMEOUT = 10;
    	
    @Override
	public StateMachine buildStateMachine() {
		StateMachine sm = new StateMachine();
		
		Action initGame = new CommonGameAction.InitGame();
		Action startPlayGame = new CommonGameAction.StartGame();
		Action completeGame = new DiceGameAction.CompleteGame();
		Action selectPlayUser = new CommonGameAction.SelectPlayUser();
		Action kickPlayUser = new CommonGameAction.KickPlayUser();
//		Action playGame = new GameAction.PlayGame();
		Action prepareRobot = new DiceGameAction.PrepareRobot();
		Action rollDiceAndBroadcast = new DiceGameAction.RollDiceAndBroadcast();
		Action broadcastRollDiceBegin = new DiceGameAction.BroadcastRollDiceBegin();
		Action broadcastNextPlayerNotification = new DiceGameAction.BroadcastNextPlayerNotification();
		Action directOpenDice = new DiceGameAction.DirectOpenDice();
		Action autoCallOrOpen = new DiceGameAction.AutoCallOrOpen();
//		
		Action setOneUserWaitTimer = new CommonGameAction.SetOneUserWaitTimer();
		Action setStartGameTimer = new CommonGameAction.CommonTimer(START_GAME_TIMEOUT, DiceTimerType.START);
		Action setRollDiceBeginTimer = new CommonGameAction.CommonTimer(ROLL_DICE_TIMEOUT, DiceTimerType.ROLL_DICE);
		Action setWaitClaimTimer = new CommonGameAction.CommonTimer(WAIT_CLAIM_TIMEOUT, DiceTimerType.WAIT_CLAIM);
		Action setShowResultTimer = new CommonGameAction.CommonTimer(SHOW_RESULT_TIMEOUT, DiceTimerType.SHOW_RESULT);		
		Action clearTimer = new CommonGameAction.ClearTimer();
		Action clearRobotTimer = new DiceGameAction.ClearRobotTimer();
		
		Action restartGame = new DiceGameAction.RestartGame();
		Action selectLoserAsCurrentPlayerUser = new DiceGameAction.SelectLoserAsCurrentPlayerUser();
		Action kickTakenOverUser = new DiceGameAction.KickTakenOverUser();
		Action clearAllUserPlaying = new DiceGameAction.ClearAllUserPlaying();
//		Action broadcastDrawUserChange = new GameAction.BroadcastDrawUserChange();

		Condition checkUserCount = new CommonGameCondition.CheckUserCount();
		
		sm.addState(INIT_STATE)		
			.addAction(initGame)
			.addAction(clearTimer)
			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)			
			.addEmptyTransition(GameCommandType.LOCAL_TIME_OUT)			
			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
			.addAction(selectPlayUser);
//			.addAction(broadcastPlayUserChange);
		
		sm.addState(new GameState(GameStateKey.CHECK_USER_COUNT))
			.setDecisionPoint(new DecisionPoint(checkUserCount){
				@Override
				public Object decideNextState(Object context){
					int userCount = condition.decide(context);
					if (userCount == 0){
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
			.addAction(startPlayGame)
			.addAction(broadcastRollDiceBegin)
			.addAction(setRollDiceBeginTimer)
//			.addTransition(GameCommandType.LOCAL_PLAY_USER_QUIT, GameStateKey.PLAY_USER_QUIT)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.ROLL_DICE_END)				
			.addAction(clearTimer);
		
		sm.addState(new GameState(GameStateKey.ROLL_DICE_END))
			.addAction(rollDiceAndBroadcast)
			.setDecisionPoint(new DecisionPoint(checkUserCount){
				@Override
				public Object decideNextState(Object context){
					int userCount = condition.decide(context);
					if (userCount == 0){
						return GameStateKey.CREATE;
					}
					else if (userCount == 1){ // only one user
						return GameStateKey.ONE_USER_WAITING;
					}
					else{ // more than one user
						return GameStateKey.WAIT_NEXT_PLAYER_PLAY;
					}
				}
			});				
		
		sm.addState(new GameState(GameStateKey.WAIT_NEXT_PLAYER_PLAY))
			.addAction(broadcastNextPlayerNotification)
			.addAction(setWaitClaimTimer)
			.addEmptyTransition(GameCommandType.LOCAL_PLAY_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
			.addTransition(GameCommandType.LOCAL_CALL_DICE, GameStateKey.PLAYER_CALL_DICE)		
			.addTransition(GameCommandType.LOCAL_OPEN_DICE, GameStateKey.CHECK_OPEN_DICE)		
			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.AUTO_ROLL_DICE)				
			.addAction(clearTimer);		

		sm.addState(new GameState(GameStateKey.PLAYER_CALL_DICE))
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session = (DiceGameSession)context;
					if (session.canContinueCall()){
						return GameStateKey.WAIT_NEXT_PLAYER_PLAY;						
					}
					else{
						return GameStateKey.DIRECT_OPEN_DICE;
					}
				}
			});			
		
		sm.addState(new GameState(GameStateKey.AUTO_ROLL_DICE))
			.addAction(autoCallOrOpen)
			.addAction(selectPlayUser)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					DiceGameSession session = (DiceGameSession)context;
					if (session.isAllUserTakenOver()){
						return GameStateKey.COMPLETE_GAME;
					}
					else if (session.isOpen()){
						return GameStateKey.COMPLETE_GAME;
					}
					else{
						return GameStateKey.WAIT_NEXT_PLAYER_PLAY;	// goto check user count state directly
					}
				}
			});	
		
		sm.addState(new GameState(GameStateKey.DIRECT_OPEN_DICE))
			.addAction(directOpenDice)
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.COMPLETE_GAME;	
				}
			});			

		
		sm.addState(new GameState(GameStateKey.CHECK_OPEN_DICE))
			.setDecisionPoint(new DecisionPoint(null){
				@Override
				public Object decideNextState(Object context){
					return GameStateKey.COMPLETE_GAME;	
				}
			});			
		
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
			.addAction(selectLoserAsCurrentPlayerUser)
			.addAction(restartGame);
		
		sm.printStateMachine();		
		return sm;
	}

}
