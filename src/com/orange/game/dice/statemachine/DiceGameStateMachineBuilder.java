package com.orange.game.dice.statemachine;

import com.orange.common.statemachine.Action;
import com.orange.common.statemachine.Condition;
import com.orange.common.statemachine.DecisionPoint;
import com.orange.common.statemachine.StateMachine;
import com.orange.common.statemachine.StateMachineBuilder;
import com.orange.game.dice.statemachine.action.GameAction;
import com.orange.game.dice.statemachine.action.GameCondition;
import com.orange.game.dice.statemachine.state.GameState;
import com.orange.game.dice.statemachine.state.GameStateKey;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;

public class DiceGameStateMachineBuilder extends StateMachineBuilder {

	// thread-safe singleton implementation
    private static DiceGameStateMachineBuilder builder = new DiceGameStateMachineBuilder();
    private static StateMachine stateMachine = builder.buildStateMachine();
    private DiceGameStateMachineBuilder(){		
	} 	
    public static DiceGameStateMachineBuilder getInstance() {         	
    	return builder; 
    } 
    	
    @Override
	public StateMachine buildStateMachine() {
//		StateMachine sm = new StateMachine();
//		
//		Action initGame = new GameAction.InitGame();
//		Action startGame = new GameAction.StartGame();
//		Action completeGame = new GameAction.CompleteGame();
//		Action selectDrawUser = new GameAction.SelectDrawUser();
//		Action kickDrawUser = new GameAction.KickDrawUser();
//		Action playGame = new GameAction.PlayGame();
//		Action prepareRobot = new GameAction.PrepareRobot();
//		Action calculateDrawUserCoins = new GameAction.CalculateDrawUserCoins();
//		Action selectDrawUserIfNone = new GameAction.SelectDrawUserIfNone();
//		
//		Action setOneUserWaitTimer = new GameAction.SetOneUserWaitTimer();
//		Action setStartGameTimer = new GameAction.SetStartGameTimer();
//		Action setWaitPickWordTimer = new GameAction.SetWaitPickWordTimer();
//		Action setDrawGuessTimer = new GameAction.SetDrawGuessTimer();
//		Action clearTimer = new GameAction.ClearTimer();
//		Action clearRobotTimer = new GameAction.ClearRobotTimer();
//		Action broadcastDrawUserChange = new GameAction.BroadcastDrawUserChange();
//
//		Condition checkUserCount = new GameCondition.CheckUserCount();
//		
//		sm.addState(GameStartState.defaultState)		
//			.addAction(initGame)
//			.addAction(clearTimer)
//			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_QUIT)			
//			.addEmptyTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT)			
//			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)			
//			.addEmptyTransition(GameCommandType.LOCAL_TIME_OUT)			
//			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
//			.addAction(selectDrawUser)
//			.addAction(broadcastDrawUserChange);
//		
//		sm.addState(new GameState(GameStateKey.CHECK_USER_COUNT))
//			.setDecisionPoint(new DecisionPoint(checkUserCount){
//				@Override
//				public Object decideNextState(Object context){
//					int userCount = condition.decide(context);
//					if (userCount == 0){
//						return GameStateKey.CREATE;
//					}
//					else if (userCount == 1){ // only one user
//						return GameStateKey.ONE_USER_WAITING;
//					}
//					else{ // more than one user
//						return GameStateKey.WAIT_FOR_START_GAME;
//					}
//				}
//			});
//		
//		sm.addState(new GameState(GameStateKey.ONE_USER_WAITING))
//			.addAction(setOneUserWaitTimer)
//			.addAction(prepareRobot)
//			.addTransition(GameCommandType.LOCAL_NEW_USER_JOIN, GameStateKey.CHECK_USER_COUNT)
//			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.CREATE)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CREATE)	
//			.addTransition(GameCommandType.LOCAL_OTHER_USER_QUIT, GameStateKey.CREATE)
//			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
//			.addAction(clearTimer)
//			.addAction(clearRobotTimer);
//		
//		sm.addState(new GameState(GameStateKey.WAIT_FOR_START_GAME))
//			.addAction(setStartGameTimer)
//			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.DRAW_USER_QUIT)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.CHECK_USER_COUNT)	
//			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
//			.addEmptyTransition(GameCommandType.LOCAL_NEW_USER_JOIN)
//			.addTransition(GameCommandType.LOCAL_START_GAME, GameStateKey.WAIT_PICK_WORD)
//			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
//			.addTransition(GameCommandType.LOCAL_DRAW_USER_CHAT, GameStateKey.WAIT_FOR_START_GAME)	
//			.addAction(clearTimer);
//		
//		sm.addState(new GameState(GameStateKey.DRAW_USER_QUIT))	
//			.addAction(selectDrawUser)
//			.addAction(broadcastDrawUserChange)			
//			.setDecisionPoint(new DecisionPoint(null){
//				@Override
//				public Object decideNextState(Object context){
//					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
//				}
//			});	
//		
//		sm.addState(new GameState(GameStateKey.KICK_DRAW_USER))
//			.addAction(kickDrawUser)
//			.addAction(selectDrawUser)
//			.addAction(broadcastDrawUserChange)
//			.setDecisionPoint(new DecisionPoint(null){
//				@Override
//				public Object decideNextState(Object context){
//					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
//				}
//			});
//		
//		sm.addState(new GameState(GameStateKey.WAIT_PICK_WORD))
//			.addAction(startGame)
//			.addAction(setWaitPickWordTimer)
//			.addTransition(GameCommandType.LOCAL_WORD_PICKED, GameStateKey.DRAW_GUESS)
//			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.COMPLETE_GAME)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
//			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
//			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.KICK_DRAW_USER)	
//			.addAction(clearTimer);
//		
//		sm.addState(new GameState(GameStateKey.DRAW_GUESS))
//			.addAction(setDrawGuessTimer)
//			.addAction(playGame)		
//			.addTransition(GameCommandType.LOCAL_DRAW_USER_QUIT, GameStateKey.COMPLETE_GAME)
//			.addTransition(GameCommandType.LOCAL_ALL_OTHER_USER_QUIT, GameStateKey.COMPLETE_GAME)	
//			.addTransition(GameCommandType.LOCAL_ALL_USER_GUESS, GameStateKey.COMPLETE_GAME)
//			.addEmptyTransition(GameCommandType.LOCAL_OTHER_USER_QUIT)
//			.addEmptyTransition(GameCommandType.LOCAL_DRAW_USER_CHAT)
//			.addTransition(GameCommandType.LOCAL_TIME_OUT, GameStateKey.COMPLETE_GAME)				
//			.addAction(clearTimer);
//		
//		sm.addState(new GameState(GameStateKey.COMPLETE_GAME))
//			.addAction(calculateDrawUserCoins)
//			.addAction(selectDrawUser)
//			.addAction(completeGame)
//			.setDecisionPoint(new DecisionPoint(null){
//				@Override
//				public Object decideNextState(Object context){
//					return GameStateKey.CHECK_USER_COUNT;	// goto check user count state directly
//				}
//			});
		
//		sm.printStateMachine();		
//		return sm;
    	
    	return null;
	}

}
