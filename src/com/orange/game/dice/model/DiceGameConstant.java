
package com.orange.game.dice.model;

import com.orange.game.constants.DBConstants;


public class DiceGameConstant {
	
	public static final String GAME_ID_DICE = DBConstants.GAME_ID_DICE;
	
	public static final int MAX_PLAYER_PER_SESSION = 6;
	
	public static final int DICE_VALUE_MAX   = 6;
	public static final int DICE_VALUE_ONE   = 1;
	public static final int DICE_VALUE_TWO   = 2;
	public static final int DICE_VALUE_THREE = 3;
	public static final int DICE_VALUE_FOUR  = 4;
	public static final int DICE_VALUE_FIVE  = 5;
	public static final int DICE_VALUE_SIX   = 6;

	
	public static final int DICE_ITEM_ROLL_DICE_AGAIN = 2001;
	public static final int DICE_ITEM_DOUBLE_COIN = 2002;
	public static final int Dice_PEEK = 2003;
	public static final int Dice_REVERSE_CALL = 2004;
	public static final int Dice_INC_TIME = 2005;
	public static final int Dice_DEC_TIME = 2006;
	public static final int Dice_CALL_HINT = 2007;
	public static final int DICE_SKIP_CALL = 2008;
	public static final int Dice_DOUBLE_KILL = 2009;
	public static final int Dice_FLOWER = 2010;
	public static final int Dice_TOMATO = 2011;


	public static final int DICE_OPEN_TYPE_NORMAL = 0;
	public static final int DICE_OPEN_TYPE_QUICK = 1;
	public static final int DICE_OPEN_TYPE_CUT = 2;

	
	public static final int DICE_WAI_FINAL_COUNT = 6;
	public static final int DICE_NET_FINAL_COUNT = 7;
	public static final int DICE_SN_FINAL_COUNT = 0;
	public static final int DICE_SNAKE_FINAL_COUNT = 0;
}
