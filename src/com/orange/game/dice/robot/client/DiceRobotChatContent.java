
package com.orange.game.dice.robot.client;

import org.apache.commons.lang.math.RandomUtils;



public class DiceRobotChatContent {
	
	// chatContent type
//	private final static int TEXT = 1;
//	private final static int EXPRESSION = 2;
	
	private final static int IDX_CONTENT = 0;
	private final static int IDX_CONTENTID = 1;
//	private final static int IDX_CONTNET_TYPE = 2;
	
	DiceRobotChatContent() {
		
	}
	private static DiceRobotChatContent chatContent = new DiceRobotChatContent();
	
	public static DiceRobotChatContent getInstance() {
		return chatContent;
	}
	
	enum  VoiceContent {
		FOLLOW_ME, 
		CHANLLENGE_YOU,
		BITE_ME,
		BELIEVE_IT,
		CALL_WILDS,
		KEEP_PLAYING,
		DONT_FOOL_ME,
		YOU_ARE_FOOL,
		I_GONNA_GO,
	}
	
	enum Expression {
		// positive meaning
		SMILE,
		HAPPY,
		PROUND,
		LOVELY,
		// negative meaning
		CRY,
		EMBARRASS,
		ANGER,
		RANDY,
		SHOCK,
		SHY,
		SLEEP,
		CRAZY,
		WORRY,
	}
	
	String[] expressionId = {
			// positive meaning, keep order consitent with Expression plz!
			"[smile]",
			"@happy@",
			"[proud]",
			"@lovely@",
			// negative meaning, keep order consitent with Expression plz!
			"@cry@",
			"[embarrass]",
			"[anger]",
			"@randy@",
			"@shock@",
			"@shy@",
			"@sleep@",
			"@crazy@",
			"[wry]",
	};
	
	String[] contentVoiceId = {
			"1", "2", "3", "4",
			"5", "6", "7", "8", "9",
	};
	
	String[] content = {
			"关注我吧。",
			"无论你叫什么，我都开！",
			"有本事你开啊！",
			"不管你信不信, 反正我是信了。",
			"大家注意, 已经叫斋了。",
			"别走啊，再来两局。",
			"想阴我，没那么容易。",
			"哈哈，你上当了。",
			"我要走啦，下次再玩。",
	};
	
	String[] englishContent = {
			"Hey, follow me.",
			"I'll chanllenge you anyway.",
			"Bite me!",
			"I just belive it",
			"Pay attention, I call wilds.",
			"Dude, keep enjoying the game.",
			"You're too foolish to fool me",
			"Hah hah, you're fool",
			"I gonna go,  hope palying with you next time",
	};
			
	public String[] getExpression(Expression expression) {
		
		String[] result = {null, null};
		
		result[IDX_CONTENT] = "NULL";
		switch (expression.ordinal()) {
			case 0:
				result[IDX_CONTENTID] = "[smile]";
				break;
			case 1:
				result[IDX_CONTENTID] =  "@happy@";
				break;
			case 2:
				result[IDX_CONTENTID] = "[proud]";
				break;
			case 3:
				result[IDX_CONTENTID] = "@lovely@";
				break;
			case 4:
				result[IDX_CONTENTID] = "@cry@";
				break;
			case 5:
				result[IDX_CONTENTID] = "[embarrass]";
				break;
			case 6:
				result[IDX_CONTENTID] = "[anger]";
				break;
			case 7:
				result[IDX_CONTENTID] = "@randy@";
				break;
			case 8:
				result[IDX_CONTENTID] = "@shock@";
				break;		
			case 9:
				result[IDX_CONTENTID] = "@shy@";
				break;
			case 10:
				result[IDX_CONTENTID] = "@sleep@";
				break;
			case 11:
				result[IDX_CONTENTID] = "@crazy@";
				break;
			case 12:
				result[IDX_CONTENTID] = "[wry]";
				break;
		}
		
		return result;
	};
	
	public String[] getExpressionByMeaning(String meaning) {
		
		String[] result = {null, null};
		result[IDX_CONTENT] = "NULL";
		int tmp = -1;
		
		if (meaning.equals("POSITIVE")) {
			tmp = RandomUtils.nextInt(4); // reference to expression array! 
		} 
		else if (meaning.equals("NEGATIVE") ) {
			tmp = RandomUtils.nextInt(9)+4;
		}
		else {
			tmp = RandomUtils.nextInt(expressionId.length);
		}
		
		result[IDX_CONTENTID] = expressionId[tmp];
		
		return result;
	}
	
	public  String[] getContent(VoiceContent voice) {
			
			// index 0 : chat content
			// index 1 : content voiceId or expressionId, depent on contentType
			String[] result = {null, null};
		
			if ( voice.ordinal() < contentVoiceId.length ) {
				result[IDX_CONTENT] = content[voice.ordinal()];
				result[IDX_CONTENTID] = contentVoiceId[voice.ordinal()];
			}
			
			return result;
	};
	
//	public String[] prepareChatContent() {
//		
//		// index 0: content( only valid for TEXT)
//		// index 1: contentVoiceId or expressionId,depent on contentType
//		// index 2: contentType
//		String[] result = {"NULL", "NULL", "NULL"};
//		
//		
//		String[] tempArray = {"NULL","NULL"};
//		int tmp = 0;
//		
//		// Expression or content
//		if ( RandomUtils.nextInt(2) == 1 ) {
//			// TEXT
//			 result[IDX_CONTNET_TYPE] = Integer.toString(TEXT);
//		    tmp = RandomUtils.nextInt(3);
//		     if ( tmp < 2) {
//		    	 tempArray = getContent(VoiceContent.CHANLLENGE_YOU);
//		     } else {
//		    	 tempArray = getContent(VoiceContent.KEEP_PLAYING);
//		      }
//		     result[IDX_CONTENT] = tempArray[IDX_CONTENT];
//		     result[IDX_CONTENTID] = tempArray[IDX_CONTENTID];
//		}
//		else {
//			// EXPRESSION
//			result[IDX_CONTNET_TYPE] = Integer.toString(EXPRESSION);
//			tmp = RandomUtils.nextInt(3);
//			if ( tmp == 0) {
//				 result[IDX_CONTENTID] = getExpression(Expression.SMILE);
//			} else if ( tmp == 1) {
//				 result[IDX_CONTENTID] = getExpression(Expression.PROUND);
//			} else if ( tmp == 2) {
//				result[IDX_CONTENTID] = getExpression(Expression.EMBARRASS);
//			} else {
//				result[IDX_CONTENTID] = getExpression(Expression.WORRY);
//			}
//		}
//		
//		return result;
//		      
//	}
	

}
