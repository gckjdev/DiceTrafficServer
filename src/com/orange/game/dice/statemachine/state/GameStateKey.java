package com.orange.game.dice.statemachine.state;

public enum GameStateKey {
	CREATE (0),
	WAITING (1),
	PLAYING (2),
	SUSPEND (3),
	FINISH (4), 

	CHECK_USER_COUNT(11), ONE_USER_WAITING(12),
	SELECT_NEW_DRAW_USER(13), WAIT_FOR_START_GAME(14), 
	KICK_PLAY_USER(15), WAIT_PICK_WORD(16), DRAW_GUESS(17), COMPLETE_GAME(18), DRAW_USER_QUIT(19),
	
	
	PLAY_USER_QUIT(20), ROLL_DICE_BEGIN(21), ROLL_DICE_END(22), 
	WAIT_NEXT_PLAYER_PLAY(23), PLAYER_CALL_DICE(24), CHECK_OPEN_DICE(25), 
	
	AUTO_ROLL_DICE(26), DIRECT_OPEN_DICE(27),
	
	
	;
	
	
	final int value;
	
	GameStateKey(int value){
		this.value = value;
	}
}
