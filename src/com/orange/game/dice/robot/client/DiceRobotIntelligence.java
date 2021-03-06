package com.orange.game.dice.robot.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.commons.lang.math.RandomUtils;

import com.orange.network.game.protocol.constants.GameConstantsProtos.DiceGameRuleType;
import com.orange.network.game.protocol.model.DiceProtos.PBDiceType;

public class DiceRobotIntelligence {
	
		private int ruleType ;
		private static final Logger logger = Logger.getLogger(DiceRobotIntelligence.class
			.getName());
		private DiceRobotChatContent chatContent = DiceRobotChatContent.getInstance();
	
		/*
		 *   多项式分布概率数组, 每一项表示n个骰子出现
		 *  x个y的概率(忽略了机器人自己的5个骰子).y可以是
		 *  1,2,3,4,5,6.   
		 *    
		 *  每一项由下列公式给出:
		 *    {n!/(x!(n-x)!)} * p^x * (1-p)^(n-x)
         *   其中,x表示出现x个1(或2,或3,或4,或5,或6)
         *   的个数, p表示每个骰子出现1(或2,或3,或4,或5,或6)的概率,
         *   它的值是1/6.
		 */
		private final  double[] probability = {
				// Two players(5 dices,except robot's 5 dices)
				0.401878,	0.401878,	0.160751,	0.0321502,	0.00321502,	0.000128601,
				
				// Three players(10 dices,except robot's 5 dices)
				0.161506,	0.323011,	0.29071,	0.155045,	0.0542659,
				0.0130238,	0.00217064, 0.000248073,
				1.86054e-05, 8.26909e-07,	1.65382e-08,
				
				// Four players(15 dices,except robot's 5 dices)
				0.0649055,	0.194716,	0.272603,	0.236256,	0.141754,	0.0623716,	0.0207905,
				0.00534613,	0.00106923,	0.000166324,	1.99589e-05,
				1.81445e-06,	1.20963e-07,	5.58291e-09,	1.59512e-10,	2.12682e-12,
				
				// Five players(20 dices,except robot's 5 dices)
				0.0260841,	0.104336,	0.198239,	0.237887,	0.202204,	0.12941,	0.0647051,	0.0258821,
				0.00841167,	0.00224311,	0.000493485,	8.97245e-05,	1.34587e-05,	1.65645e-06,
				1.65645e-07,	1.32516e-08,	8.28226e-10,	3.89753e-11,	1.29918e-12,
				2.73511e-14,	2.73511e-16,
				
				// Six players(25 dices, except robot's 5 dices)
				0.0104826,	0.052413,	0.125791,	0.19288,	0.212168,	0.178221,	0.118814,	0.064499,
				0.0290245,	0.0109648,	0.00350875,	0.000956931,	0.000223284, 4.46568e-05,	7.65544e-06,
				1.1228e-06,1.4035e-07,	1.48606e-08,	1.32094e-09,	9.73324e-11,	5.83994e-12,
				2.78093e-13,	1.01125e-14,	2.63803e-16,	4.39672e-18,	3.51738e-20,
		};
		
		// In case of two players, base in probability[] is 0, etc.
		private final static int[]  BASE = {0, 6, 17, 33, 54}; 

		/*
		 * 5(,10,15,20,25)个骰子中出现1(或2,...或6)的个数的期望值M,
		 * 		期望值公式: M = ∑ k*pk
		 *  	k表示出现0次(1次,2次.....25次)
		 * 	pk是出现k次的对应概率,即上面的probablity数组
		 * 		∑是连加. 如：
		 *   6项(5个骰子)相加算出来是3.302, 四舍五入成3.
		 */
		private  static final  int  DiceMeanValue[] = {3, 5, 5, 5, 5};
		

		// If difference is >= unsafe_difference[playerCount-2], it's not safe
		private final static int[] UNSAFE_DIFFERENCE = {3, 6, 8, 9, 10};
		// Initial benchmark probability
		// 5:  0.160751;   10: 0.0542659;  15: 0.0207905;  20: 0.0258821;	25: 0.0109648
		private static final double INI_BENCHMARK[] = {0.16, 0.05, 0.02,	0.02,  0.01} ;
		// Current game's benchmark probablity
		private double[]  benchmark = {INI_BENCHMARK[0], INI_BENCHMARK[1], INI_BENCHMARK[2], INI_BENCHMARK[3], INI_BENCHMARK[4]};
		
		private  void resetBenchmark() {
			benchmark[0] = INI_BENCHMARK[0]; benchmark[1] = INI_BENCHMARK[1]; benchmark[2] = INI_BENCHMARK[2];
			benchmark[3] = INI_BENCHMARK[3]; benchmark[4] = INI_BENCHMARK[4];
		}
		
		// Robot's highest intelligence
		private static final double HIGHEST_IQ = 1;
		// IQ threshold affects how robot make decision
		private static final double IQ_THRESHOLD = 0.6d;
		// A accelerator fator that control how
		// fast the intelligence changes
		private static final double ACCELERATOR_FACTOR = Math.E / 2;
		// Robot's IQ in current game
		private  double intelligence;
		
//      *** NOT IMPLEMENTED NOW !!! ***
//		// Per-player's initial honesty
//		private static final int INIHONESTY = 10;
//		// At most five player(except robot itself) 
//		private int[] honesty = {INIHONESTY,INIHONESTY,INIHONESTY,INIHONESTY,INIHONESTY};
		
		// The current this game in
		private int round;
		// How many games the robot wins
		private int winGame;
		// How many games the robot loses
		private int loseGame;
		// Last Game playercount, it is 2 in the beginning.
		private int lastPlayerCount = 2;
	
		// A flag to indicate whether giving up calling.
		private boolean giveUpCalling = false;
		
		// An array where we put what to call.
		// index 0: the number of dices
		// index 1: which dice
		// index 2: is wild ?
		private final static int IDX_NUM_OF_DICE 		= 0;
		private final static int IDX_DICE_FACE_VALUE = 1;
		private final static int IDX_CALL_WILD 		= 2;
		private int[] whatToCall = {0, 0, 0};
		private int[] lastRoundCall = {0, 0, 0};
		private boolean catCut;
		
		// What each player last calls
		private Map<String, Integer> lastCall = new HashMap<String, Integer>();
		// Does player change his/her dice, compared to last round?
		private Map<String, Boolean> changeDiceValue = new HashMap<String, Boolean>();
		
		/* Introspection of robot's dices, set by introspectRobotDices method.
		 * index 0: any dice of 4 or 5 instances ? 1 for yes, 0 for no.
		 * index 1: which dice has 4/5 instances ? Only set if index 0 is 1.
		 * index 2: any dice of 3 instances ? 1 for yes, 0 for no.
		 * index 3: which dice has 3 instances ? Only set if index 3 is 1.
		 * index 4: any dice of 2 instances ? 1 for yes, 0 for no.
		 * index 5: which dice has 3 instances ? Only set if index 5 is 1.
		 * index 6: same as index 6(There may be two dices of 2 instances).
		 * index 7: distributed uniformly.  
		 */
		private final static int NUM_MORE_THAN_FOUR 	= 0;
		private final static int DICE_MORE_THAN_FOUR = 1;
		private final static int NUM_OF_THREE 			= 2;
		private final static int DICE_OF_THREE 		= 3;
		private final static int NUM_OF_TWO 			= 4;
		private final static int DICE_OF_TWO 			= 5;
		private final static int ANOTHER_DICE_OF_TWO = 6;
		private final static int DISTRIBUTE_UNIFORMLY= 7;
		private int[] introspection = {0, 0, 0, 0, 0, 0, 0, 0};
		
		// How dose robot's dices distribute?
		private final static int DICE_VALUE_ONE 	= 1;
		private final static int DICE_VALUE_TWO 	= 2;
		private final static int DICE_VALUE_THREE = 3;
		private final static int DICE_VALUE_FOUR 	= 4;
		private final static int DICE_VALUE_FIVE 	= 5;
		private final static int DICE_VALUE_SIX 	= 6;
		private int[] distribution = {0, 0, 0, 0, 0, 0};
		
		// Dice type
		private PBDiceType diceType;
		// For special dice : WAI NET
		private int diceValue;
		
		// Is it safe for robot to call?
		private boolean safe = true;
		// Does robot lie?
		private boolean lying = false;
		// If lie, what dice it lie?
		private int lieDice = 0;
		
		// In one game, we limit robot to only send one callwild message.
//		private boolean hasSendCallWilds = false;
		
		
		// For chat.
		private final static int TEXT = 1;
		private final static int EXPRESSION = 2;
		/*
		 * index 0 : chatContent
		 * index 1 : chatVoidId
		 * index 2 : contentType: TEXT or EXPRESSION
		 */
		private final static int IDX_CONTENT 		= 0;
		private final static int IDX_CONTENTID 	= 1;
		private final static int IDX_CONTNET_TYPE = 2;
		private String[ ] whatToChat = {"关注我吧。","1", "1"};
		private boolean setChat = false;
		private boolean canCut;
		
		
		private void reset(int[] array) {
			for ( int i = 0; i< array.length; i++) {
				array[i] = 0 ;
			}
		}
		
		public DiceRobotIntelligence(int rule) {
				intelligence = HIGHEST_IQ;
				round = 0;
				winGame = 0;
				loseGame = 0;
				ruleType = rule;
		}
		
		// Mainly adjust robot's IQ and the probability benchmark.
		public void balanceAndReset(int playerCount, boolean robotWinThisGame) {
			
			double tmp = 0.0;
			
			if ( robotWinThisGame ) {
				winGame++;
				intelligence = HIGHEST_IQ / Math.pow(ACCELERATOR_FACTOR, winGame);
				if ( playerCount == lastPlayerCount ) {
					tmp = benchmark[playerCount-2] / intelligence;
					benchmark[playerCount-2] = ( tmp > 0.6 ? 0.6 : tmp );
				} else {
					resetBenchmark();
				}
				loseGame = 0;
			} else {
				loseGame++;
				if ( intelligence * Math.pow(ACCELERATOR_FACTOR, loseGame) > HIGHEST_IQ)
					intelligence = HIGHEST_IQ;
				else 
					intelligence *= Math.pow(ACCELERATOR_FACTOR, loseGame);
				if ( playerCount == lastPlayerCount ) {
					tmp = benchmark[playerCount-2] / intelligence;
					benchmark[playerCount-2] = (tmp > 0.6 ? 0.6 : tmp);
				} else {
					resetBenchmark();
				}
				winGame = 0;
			}
			lastPlayerCount = playerCount;
			round = 0;
			giveUpCalling = false;
			safe = true;
			lying = false;
			lieDice = 0;
			setChat = false;
			canCut = false;
//			hasSendCallWilds = false;
			reset(whatToCall);
			reset(introspection);
			reset(distribution);
		}
		
		public boolean canOpenDice(String nickName, int playerCount,String userId, int num, int dice, boolean isWild) {
			
			boolean canOpen = false;
			if (userId == null){
				return false;
			}
			
			int notWild = (isWild == false? 1 : 0);
			// How many "dice" robot have.
			int numOfDice = howManyDices(dice, notWild);
			int difference = num - numOfDice;
			
			Integer diceInteger = new Integer(dice);
			if ( lastCall.containsKey(userId) == false ) {
				lastCall.put(userId, diceInteger);
				changeDiceValue.put(userId, new Boolean(false));
			} else {
				if ( lastCall.get(userId).equals(diceInteger) == false ) {
					changeDiceValue.put(userId, new Boolean(true));
					lastCall.put(userId, diceInteger);
				} else {
					changeDiceValue.put(userId, new Boolean(false));
				}
			}

			// Make a decision...
			if ( intelligence < IQ_THRESHOLD  && ruleType == DiceGameRuleType.RULE_NORMAL_VALUE ) {
				canOpen = ((int)HIGHEST_IQ/intelligence >= 2 && difference > UNSAFE_DIFFERENCE[playerCount-2] ? true : false);
				catCut = ( difference > (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
				if(canOpen) {
					logger.info("Robot["+nickName+"] is not smart, it decides to open!");
//					setChatContent(TEXT,chatContent.getContent(DiceRobotChatContent.VoiceContent.DONT_FOOL_ME));
				}
				return canOpen;
			}
			
			// Ok, below starts hard work, since robot is quite smart ^_^
			// lying means robot call a dice value that it even doesn't have one!
			if ( lying && dice == lieDice && difference >= UNSAFE_DIFFERENCE[playerCount-2]) {
					canOpen = RandomUtils.nextInt(2) == 1? true : false;
					catCut = ( difference > (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
					logger.info("Robot["+nickName+"] is lying and player is fooled,open!");
//					setChatContent(TEXT,chatContent.getContent(DiceRobotChatContent.VoiceContent.YOU_ARE_FOOL));
					setChatContent(EXPRESSION, chatContent.getExpression(DiceRobotChatContent.Expression.PROUND));
					return canOpen;
			}
			
			// If difference <= 0, of course robot won't chanllenge.
			if ( difference > 0 ) {
				if ( difference > UNSAFE_DIFFERENCE[playerCount-2] ) {
					canOpen = true;
					catCut = ( RandomUtils.nextInt(2) == 0 && difference > (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
					logger.info("Robot["+nickName+"]: Call to much, open!");
				}
				// Distributed uniformly & quantity is too big, it's not safe to call.
				else if ( introspection[DISTRIBUTE_UNIFORMLY] == 1 && difference >= UNSAFE_DIFFERENCE[playerCount-2]) {
					canOpen = true;
					catCut = ( RandomUtils.nextInt(2) == 0 && difference > (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
					logger.info("Robot["+nickName+"]: Distributed uniformly & call too much, open!");
				}
				else if ( probability[BASE[playerCount-2] + difference]  < benchmark[playerCount-2] ) {
					if ( round <= 2 ){
						canOpen = ( difference > UNSAFE_DIFFERENCE[playerCount-2] ?  true : false );
						catCut = ( RandomUtils.nextInt(2) == 0 && difference > (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
						if (canOpen)
							logger.info("Robot["+nickName+"]: round <=2, call too much, open!");
					}
					if (round == 2 || round == 3) {
						if ( changeDiceValue.get(userId) == true) {
							canOpen = (round + RandomUtils.nextInt(2) > 2 ? true : false);
							catCut = ( RandomUtils.nextInt(2) == 0 && difference >= (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
							if(canOpen) {
								logger.info("Robot["+nickName+"]: round 2 or round 3, player changes dice face value, he/she may be cheating, open!");
//								setChatContent(TEXT,chatContent.getContent(DiceRobotChatContent.VoiceContent.DONT_FOOL_ME));	
								return canOpen;
							}
						}
						if ( difference >= UNSAFE_DIFFERENCE[playerCount-2] ) {
							canOpen = (round + RandomUtils.nextInt(2) > 2 ? true : false);
							catCut = ( RandomUtils.nextInt(2) == 0 && difference >= (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
							if(canOpen)
								logger.info("Robot["+nickName+"]: round 2 or round 3, call too much, open!");
						}
						else if ( !safe ){
							canOpen = (RandomUtils.nextInt(2) == 1 ? true : false );
							catCut = ( RandomUtils.nextInt(2) == 0 && difference >= (UNSAFE_DIFFERENCE[playerCount-2]+1) ? true : false);
							if(canOpen)
								logger.info("Robot["+nickName+"]: Not safe, open!");
						}
					}
					else if ( round > 4) {
						canOpen = true;
						catCut = ( RandomUtils.nextInt(2) == 0 && difference >= (UNSAFE_DIFFERENCE[playerCount-2]) ? true : false);
						logger.info("Robot["+nickName+"]: Too much round, calling is dangerous, open!");
					}
				}	
			}
			// For chat
			if (canOpen == true && RandomUtils.nextInt(3) == 1) {
//				setChatContent(TEXT,chatContent.getContent(DiceRobotChatContent.VoiceContent.BELIEVE_IT));
				if ( RandomUtils.nextInt(2) == 0 )
					setChatContent(EXPRESSION, chatContent.getExpression(DiceRobotChatContent.Expression.SMILE));
				else 
					setChatContent(EXPRESSION, chatContent.getExpression(DiceRobotChatContent.Expression.PROUND));
			} 
				
			
			return canOpen;
		}

		
	public void decideWhatToCall(String nickName, int playerCount,int num, int dice, boolean isWild) {
		
			int tmp = 0;
			
			giveUpCalling = false;
			whatToCall[IDX_NUM_OF_DICE] = 0;
			whatToCall[IDX_DICE_FACE_VALUE] = 0;
			whatToCall[IDX_CALL_WILD] = 0;
		
			// We are first to call 
			if ( num == -1 || dice == -1) {
				intialCall(nickName, playerCount);
				return;
			}
			
			// Just adding one even exceeds the limit, we should not call. 
			if ( num + 1 >= playerCount * 5 ) {
				giveUpCalling = true;
				return ;
			}
			
			logger.info("Current round is Round " + (round +1)+". Robot["+nickName+"]'s IQ is " + intelligence 
					+". PlayerCount is " + playerCount+". Benchmark is " + benchmark[playerCount-2]);
		
			int notWild = (isWild == false? 1 : 0);
			// How many "dice" robot have.
			int numOfDice = howManyDices(dice, notWild);
			int difference = num - numOfDice;

			
			// Make decision...
			if (isWild){
				// Not so intelligent, just add 1
				if ( intelligence < IQ_THRESHOLD && ruleType == DiceGameRuleType.RULE_NORMAL_VALUE){
					if (num + 1 - distribution[dice-1] <= UNSAFE_DIFFERENCE[playerCount-2] ) {
						recordCall(num+1, dice,1, playerCount);
						safe = false;
						logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & not so smart, just add one, call "
								+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
					} else {
						giveUpCalling = true;
						logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & not so smart, not safe to call, give up!");
						return;
					}
				}
				// Quite smart, do some deep thought.
				else {
					// We don't have as many dices as called.
					if ( difference > 0 ) {
						for( int i= 0; i < distribution.length; i++ ) {
							if ( i + 1 > dice && distribution[i] >= difference ) {
								recordCall(num + (dice == DICE_VALUE_ONE ? 1 : 0), i+1, 1,playerCount);
								round++;
								logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, change dice, call"
										+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
								return;
							}
							else if ( i+1 <= dice && distribution[i] > difference ){
								recordCall(num + 1, i+1, 1,playerCount);
								round++;
								logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, change dice, call"
										+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
								return;
							}
						}
						// round+1 is current round, if current round is 3, it must be true,
						// if current round is 2, it is 50% true. 
						if ( difference+1 < UNSAFE_DIFFERENCE[playerCount-2] && (round == 0 || round + 1 + RandomUtils.nextInt(2) <= 2) ){
							recordCall(num+1, dice,1,playerCount);
							safe = false;
							logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, just add one, call "
									+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
						} else {
							logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, but not safe to call, give up calling");
							giveUpCalling = true;
							round++;
							return;
						}
					} 
					// We have more dices than that called.
					else {
						// Some dice has more than 4 instances...
						if ( introspection[NUM_MORE_THAN_FOUR] == 1 ) {
							recordCall((introspection[DICE_MORE_THAN_FOUR] > dice && dice != DICE_VALUE_ONE ? num : num +1),
									introspection[DICE_MORE_THAN_FOUR], 1,playerCount);
								logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, "+introspection[DICE_MORE_THAN_FOUR] 
										+ "has more than 4 instances, so change dice to " + introspection[DICE_MORE_THAN_FOUR]+
										", call "+ whatToCall[IDX_NUM_OF_DICE] + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
						// Some dice has 3 instances...
						} else if ( introspection[NUM_OF_THREE] == 1 ) {
							recordCall((introspection[DICE_OF_THREE] > dice && dice != DICE_VALUE_ONE? num : num +1),
									introspection[DICE_OF_THREE], 1,playerCount);
							logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild & smart, "+introspection[DICE_OF_THREE] 
									+ "has 3 instances, so change dice to " + introspection[DICE_OF_THREE]+
									", call "+ whatToCall[IDX_NUM_OF_DICE] + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
						}
						else {
							recordCall(num+1, dice, 1, playerCount);
							safe = false;
							logger.info("<DiceRobotIntelligence>Robot["+nickName+"] isWild &  smart, just add one, call "
									+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
						}
					}
				}
			} // end of if(isWilds) 
			// Not wild~
			else {
				int quotient = (int)(IQ_THRESHOLD/intelligence);
				int extra;
				if ( ruleType != DiceGameRuleType.RULE_NORMAL_VALUE ) {
					extra = 0;
				} else {
					extra = (num > playerCount*5* 2/3)? 0 : 
					  (quotient ==0 ? 0 : (quotient > 3 ? 1+RandomUtils.nextInt(2) + (playerCount/3) : (1 + playerCount/4 )/(round+1)+ RandomUtils.nextInt(2))); 
				}
				
				// Does robot have more than 3 ONEs?
				if ( distribution[DICE_VALUE_ONE-1] >= 3 ){
						// YES, call ONE(auto wild)
					if ( num <= DiceMeanValue[playerCount-2] + distribution[DICE_VALUE_ONE-1] && probability[BASE[playerCount-2] + num] > benchmark[playerCount-2] ) {
							recordCall(num + extra, 1, 1, playerCount);
							safe = false;
							logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, many ONEs, call one! Call "
									+ whatToCall[IDX_NUM_OF_DICE]  + " X " + dice );
					} else {
							// YES, but the num is some little big, call wilds is not safe,
							// we should be careful.
							// 3 X 1 + 2 X ?
							if ( introspection[NUM_OF_TWO] == 1) {
								recordCall((dice > introspection[DICE_OF_TWO] ? num+1 : num)+extra, introspection[DICE_OF_TWO], 0, playerCount);
								safe = false;
 								logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, many ONEs & has dice of 2 instances, call "
 										+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
							}
							else {
								tmp = probability[BASE[playerCount-2] + num + 2] > benchmark[playerCount-2]? num + 2 : num + 1;
								recordCall(tmp+extra, dice, 0, playerCount);
								logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, many ONEs, but not safe to call wilds, call "
										+ whatToCall[IDX_NUM_OF_DICE]  + " X " + dice );
							}
					}
						
				} 
				// We have any dice of more than 4 instances, change dice...
				else if ( introspection[NUM_MORE_THAN_FOUR] == 1 ) {
					recordCall((dice >= introspection[DICE_MORE_THAN_FOUR] ? num + 1 : num)+extra, introspection[DICE_MORE_THAN_FOUR], 0, playerCount);
					logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, has more than 4 "+introspection[DICE_MORE_THAN_FOUR]+
							", so change dice to " + introspection[DICE_MORE_THAN_FOUR]+", call "+ whatToCall[IDX_NUM_OF_DICE] 
									+ " X " + whatToCall[IDX_DICE_FACE_VALUE]);
				}
				// We have dice of 3 instances...(not ONE, otherwise this branch won't be executed)
				else if ( introspection[NUM_OF_THREE] == 1 ){ //&& distribution[DICE_VALUE_ONE-1] == 2) {
					if ( introspection[DICE_OF_THREE] + distribution[DICE_VALUE_ONE-1]* notWild + UNSAFE_DIFFERENCE[playerCount-2] <= num+1) {
						recordCall(num + 1 + distribution[DICE_VALUE_ONE-1], introspection[DICE_OF_THREE], 0, playerCount);
						logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, has 3 "+introspection[DICE_OF_THREE]+ ", call "
								+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
					} else {
						giveUpCalling = true;
						logger.info("<DiceRobotIntelligence> Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ 
								"smart,has 3 instances of dice, but not safe to call, give up calling");
						return;
					}
				}
				// We have dice of 2 intances...
				else if ( introspection[NUM_OF_TWO] == 1) {
					if (introspection[ANOTHER_DICE_OF_TWO] != 0  ) {
						if ( introspection[ANOTHER_DICE_OF_TWO] == DICE_VALUE_ONE || introspection[DICE_OF_TWO] == DICE_VALUE_ONE) {
							if ( introspection[ANOTHER_DICE_OF_TWO] == DICE_VALUE_ONE ) {
								recordCall(num + (introspection[DICE_OF_TWO]> dice ? 0 : 1)+extra,introspection[DICE_OF_TWO], 0, playerCount);
							} else {
								recordCall(num + (introspection[ANOTHER_DICE_OF_TWO]> dice ? 0 : 1)+extra,introspection[ANOTHER_DICE_OF_TWO], 0, playerCount);
							}
							logger.info("<DiceRobotIntelligence>(1) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, has 2 X "+introspection[DICE_OF_TWO]+ " and 2 X 1, call "
									+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
							round++;
							return;
						}
						else if ( introspection[ANOTHER_DICE_OF_TWO] > dice ) {
							recordCall(num+extra, introspection[ANOTHER_DICE_OF_TWO], 0, playerCount);
						}
						else if ( introspection[DICE_OF_TWO] > dice ) {
							recordCall(num+extra, introspection[DICE_OF_TWO], 0, playerCount);
						}
						else if ( num+1 - distribution[introspection[ANOTHER_DICE_OF_TWO]-1] - distribution[DICE_VALUE_ONE-1] < UNSAFE_DIFFERENCE[playerCount-2] ) {
							recordCall(num+1+extra, introspection[ANOTHER_DICE_OF_TWO], 0, playerCount);
						}
						else if ( num+1 - distribution[introspection[DICE_OF_TWO]-1] - distribution[DICE_VALUE_ONE-1] < UNSAFE_DIFFERENCE[playerCount-2] ) {
							recordCall(num+1+extra, introspection[DICE_OF_TWO], 0, playerCount);
						}
						else {
							giveUpCalling = true;
							logger.info("<DiceRobotIntelligence>(2) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ 
									"smart,has 2 instances of dice, but not safe to call, give up calling");
							return;
						}
						logger.info("<DiceRobotIntelligence>(3) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, has 2 X "+introspection[ANOTHER_DICE_OF_TWO]+ ", call "
								+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
					} 
					else {
						if ( introspection[DICE_OF_TWO] == DICE_VALUE_ONE ) {
							for ( int i = distribution.length; i >= 1; i-- ) {
								if ( distribution[i-1] != 0 && probability[BASE[playerCount-2] + num+1-(introspection[DICE_OF_TWO]+1)] > benchmark[playerCount-2]) {
									recordCall(num+1+extra, distribution[i-1], 0, playerCount);
									logger.info("<DiceRobotIntelligence>(4) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart,has 2 X 1, call "
											+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
									round++;
									return;
								}
							}
							recordCall(num+1+extra, dice, 0, playerCount);
							logger.info("<DiceRobotIntelligence>(5) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart, has 2 X 1, call "
									+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
						}
						else if ( introspection[DICE_OF_TWO] > dice ) {
							recordCall(num+extra, introspection[DICE_OF_TWO], 0, playerCount);
						}
						else if ( num+1 - distribution[introspection[DICE_OF_TWO]-1] - distribution[DICE_VALUE_ONE-1] < UNSAFE_DIFFERENCE[playerCount-2] ) {
							recordCall(num+1+extra, introspection[DICE_OF_TWO], 0, playerCount);
						}
						else {
							giveUpCalling = true;
							logger.info("<DiceRobotIntelligence>(6) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ 
									"smart,has 2 X "+introspection[DICE_OF_TWO]+ ", but not safe to call, give up calling");
							return;
						}
						logger.info("<DiceRobotIntelligence>(7) Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ 
								"smart, has 2 X "+introspection[ANOTHER_DICE_OF_TWO]+ ", call "
								+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
					}
				
				}
				// We have our dices distributed uniformly.
				else {
					if ( ruleType == DiceGameRuleType.RULE_NORMAL_VALUE &&  num+1 - 2 < UNSAFE_DIFFERENCE[playerCount-2]) {
						recordCall(num + 1, dice, 0, playerCount);
						logger.info("<DiceRobotIntelligence>Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ "smart,dices distributed uniformly, just do a safe call , call "
							+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
					} else {
						giveUpCalling = true;
						logger.info("<DiceRobotIntelligence> Robot["+nickName+"] not Wild & " + (intelligence< IQ_THRESHOLD? "not ":"")+ 
								"smart,dices distributed uniformly, give up calling");
						return;
					}
				}
			} // end of not Wild
			
			round++;
		}


	private int howManyDices(int dice, int notWild) {
		
		int numOfDice = distribution[dice-1] + ( dice != DICE_VALUE_ONE ? distribution[DICE_VALUE_ONE-1] * notWild : 0);
		// If robot gets SNAKE DICE ? 
		if ( ruleType != DiceGameRuleType.RULE_NORMAL_VALUE ) {
			if (diceType.equals(PBDiceType.DICE_SNAKE)) {
				numOfDice = 0;
			} else if ( diceType.equals(PBDiceType.DICE_WAI) && diceValue == dice && notWild == 1) {
				numOfDice = 6;
		   } else if (diceType.equals(PBDiceType.DICE_NET) && diceValue == dice ) {
				numOfDice = 7;
		    }
		}
		return numOfDice;
	}

	// Record what robot wants to call
	private void recordCall(int num, int dice, int isWild,int playerCount) {
		
		// We should avoid call the same as last round
		// (eg: robot calls 3x4, player calls 3x1, robot may call 3x4 again, which is not permitted)
		// Just add the quantity by one
		if ( lastRoundCall[IDX_NUM_OF_DICE] == num && lastRoundCall[IDX_DICE_FACE_VALUE] == dice  ) {
				num++;
		}
		
		whatToCall[IDX_NUM_OF_DICE] = lastRoundCall[IDX_NUM_OF_DICE] = num;
		whatToCall[IDX_DICE_FACE_VALUE] = lastRoundCall[IDX_DICE_FACE_VALUE] = dice;
		// If callNum is the same as playerCount , auto wild,
		// If dice is ONE,no doubt it is  wild.  
		if ( whatToCall[IDX_NUM_OF_DICE] == playerCount || whatToCall[IDX_DICE_FACE_VALUE] == DICE_VALUE_ONE) {
			whatToCall[IDX_CALL_WILD] = lastRoundCall[IDX_CALL_WILD] = 1;
		} else {
			whatToCall[IDX_CALL_WILD] = lastRoundCall[IDX_CALL_WILD] = isWild;
		}
//		if ( whatToCall[IDX_CALL_WILD] == 1 && hasSendCallWilds == false && RandomUtils.nextInt(2) == 1) {
//			logger.info("*****Robot call wilds! Set chat content*****");
//			setChatContent(TEXT,chatContent.getContent(DiceRobotChatContent.VoiceContent.CALL_WILDS));
//			hasSendCallWilds = true;
//		} else if ( RandomUtils.nextInt(4) == 1 && safe == true ) {
//			setChatContent(TEXT,chatContent.getContent(DiceRobotChatNot Wild &  smartContent.VoiceContent.BITE_ME));
//		}
		if ( RandomUtils.nextInt(3) == 1 ) {
			if ( safe == true ) {
				setChatContent(EXPRESSION, chatContent.getExpressionByMeaning("POSITIVE"));
			} 
			else if ( RandomUtils.nextInt(2) == 0 ) {
				setChatContent(EXPRESSION, chatContent.getExpression(DiceRobotChatContent.Expression.SMILE));
			}
			else {
				setChatContent(EXPRESSION, chatContent.getExpression(DiceRobotChatContent.Expression.EMBARRASS));
			}
		}
	}

	// Robot initiates the call.
	private void intialCall(String nickName,int playerCount) {
		
		int tmp = (RandomUtils.nextInt(5) == 0? 0 : 1);
		
		if ( intelligence < IQ_THRESHOLD&& ruleType == DiceGameRuleType.RULE_NORMAL_VALUE){
			recordCall( playerCount + tmp, 1+RandomUtils.nextInt(6), 0, playerCount );
			safe = false;
			logger.info("<intialCall> Initial call, Robot["+nickName+"] not smart, just do a random call , call "
					+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
			if ( distribution[whatToCall[IDX_DICE_FACE_VALUE]-1] == 0 ) {
				lying  = true;
				lieDice  = whatToCall[IDX_DICE_FACE_VALUE];
			}
		}
		// Smart...
		else {
			// Does robot have more than 3 ONEs?
			if ( distribution[DICE_VALUE_ONE-1] >= 3 ){
				if ( RandomUtils.nextInt(2) == 1 && playerCount-distribution[DICE_VALUE_ONE] < UNSAFE_DIFFERENCE[playerCount-2]) {
					recordCall(playerCount, DICE_VALUE_ONE, 1, playerCount);
					safe = false;
				} else {
					recordCall(playerCount+tmp, RandomUtils.nextInt(5)+2, 0, playerCount);
					safe = false;
				} 
				logger.info("<intialCall> Initial call,Robot["+nickName+"] smart, has more than 3 ONEs,just call ONE or do a random call , call "
						+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE] );
			}
			else if ( introspection[NUM_MORE_THAN_FOUR] == 1 ) {
				recordCall(playerCount+tmp, introspection[DICE_MORE_THAN_FOUR], 0, playerCount);
				logger.info("<intialCall> Initial call,Robot["+nickName+"] has more than 4 " + introspection[DICE_MORE_THAN_FOUR]+
						", so call "+ whatToCall[IDX_NUM_OF_DICE] + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
			}
			else if ( introspection[NUM_OF_THREE] == 1) {
				recordCall(playerCount+tmp, introspection[DICE_OF_THREE], RandomUtils.nextInt(4) == 1? 1: 0, playerCount);
				logger.info("<intialCall> Initial call,Robot["+nickName+"]has 3 " + introspection[DICE_OF_THREE]+
						", so call "+ whatToCall[IDX_NUM_OF_DICE] + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
			}
			else if ( introspection[NUM_OF_TWO] == 1 ){
				if ( introspection[ANOTHER_DICE_OF_TWO] != 0 ) {
					int quantity = 2 + (introspection[ANOTHER_DICE_OF_TWO] != DICE_VALUE_ONE ? distribution[DICE_VALUE_ONE-1] : 0); 
					recordCall( (quantity >= playerCount ? quantity : playerCount+1), 
							introspection[ANOTHER_DICE_OF_TWO], 0, playerCount);
				}
				else {
					int quantity = 2 + (introspection[DICE_OF_TWO] != DICE_VALUE_ONE ? distribution[DICE_VALUE_ONE-1] : 0);
					recordCall((quantity >= playerCount ? quantity : playerCount+1) ,
							introspection[DICE_OF_TWO], 0, playerCount);
				}
				logger.info("<intialCall> Initial call,Robot["+nickName+"] has dice of 2 instances, so call "
						+ whatToCall[IDX_NUM_OF_DICE] + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
			}
			else {
				recordCall(playerCount + tmp, 2 + RandomUtils.nextInt(5), (RandomUtils.nextInt(10) == 1? 1 : 0), playerCount);
				safe = false;
				logger.info("<intialCall> Initially, Robot["+nickName+"] smart,dices distributed uniformly,  do a random call , call "
						+ whatToCall[IDX_NUM_OF_DICE]  + " X " + whatToCall[IDX_DICE_FACE_VALUE]);
				if ( distribution[whatToCall[IDX_DICE_FACE_VALUE]-1] == 0 ) {
					lying  = true;
					lieDice  = whatToCall[IDX_DICE_FACE_VALUE];
				}
			}
		}
		round++;
	}
		
		public boolean giveUpCall() {
			return giveUpCalling;
		}
		
		public int[] getWhatTocall() {
			
			int[] result = {0, 0, 0};
			
			result[IDX_NUM_OF_DICE] = whatToCall[IDX_NUM_OF_DICE];
			result[IDX_DICE_FACE_VALUE] = whatToCall[IDX_DICE_FACE_VALUE];
			result[IDX_CALL_WILD] = whatToCall[IDX_CALL_WILD];
			
			return result;
		}
		
		
		public void introspectRobotDices(int[] robotDices) {
			
			logger.info("robot's Dices are: "+ robotDices[0]+", " +robotDices[1]
					+", " + robotDices[2]+", "+ robotDices[3]+", " +
					robotDices[4]);
			
			for ( int i= 0; i < robotDices.length;i++ ) {
				switch (robotDices[i]) {
				case DICE_VALUE_ONE:
					distribution[DICE_VALUE_ONE-1]++;
					break;
				case DICE_VALUE_TWO:
					distribution[DICE_VALUE_TWO-1]++;
					break;
				case DICE_VALUE_THREE:
					distribution[DICE_VALUE_THREE-1]++;
					break;
				case DICE_VALUE_FOUR:
					distribution[DICE_VALUE_FOUR-1]++;
					break;
				case DICE_VALUE_FIVE:
					distribution[DICE_VALUE_FIVE-1]++;
					break;
				case DICE_VALUE_SIX:
					distribution[DICE_VALUE_SIX-1]++;
					break;
				default:
					break;
				}
				
			}
			for (int i = 0;i < distribution.length; i++) {
				if ( distribution[i] >= 4 ) {
					introspection[NUM_MORE_THAN_FOUR] = 1;
					introspection[DICE_MORE_THAN_FOUR] = i+1;
				} 
				else if ( distribution[i] == 3 ) {
					introspection[NUM_OF_THREE] = 1;
					introspection[DICE_OF_THREE] = i+1;
				}
				else if ( distribution[i] == 2 ) {
					introspection[NUM_OF_TWO] = 1;
					if ( introspection[DICE_OF_TWO] == 0 ) {
						introspection[DICE_OF_TWO] = i+1;
					}
					else {
						introspection[ANOTHER_DICE_OF_TWO] = i+1;
					}
				}
				
			}// end of for
			
			if ( introspection[NUM_MORE_THAN_FOUR] == 0 && introspection[NUM_OF_THREE] == 0 && introspection[NUM_OF_TWO] == 0) {
					introspection[DISTRIBUTE_UNIFORMLY] = 1;
			}
			
			// which dice type
			int count = 0; // dice category count
			for ( int i = 0; i < distribution.length; i++ ) {
				if ( distribution[i] != 0 ) 
					count++;
					diceValue = i+1;
			}
			if ( count == 1 ) 
				diceType = PBDiceType.DICE_NET;
			else if ( count == 2 && distribution[DICE_VALUE_ONE-1] != 0)
				diceType = PBDiceType.DICE_WAI;
			else if ( count == 5 )
				diceType = PBDiceType.DICE_SNAKE;
			else 
				diceType = PBDiceType.DICE_NORMAL;

		}
		
		public boolean hasSetChat() {
			return setChat;
		}
		
		public void resetHasSetChat() {
			this.setChat = false;
		}
		
		public String[] getChatContent(){
			String[] result = {null,null, null};
			
			result[IDX_CONTENT] = whatToChat[IDX_CONTENT];
			result[IDX_CONTENTID] = whatToChat[IDX_CONTENTID];
			result[IDX_CONTNET_TYPE]= whatToChat[IDX_CONTNET_TYPE];
			
			return result;
		}
		
		private void setChatContent(int contentType,String[] content) {
			
			this.setChat = true;
			
			whatToChat[IDX_CONTENT] = content[IDX_CONTENT];
			whatToChat[IDX_CONTENTID] = content[IDX_CONTENTID];
			whatToChat[IDX_CONTNET_TYPE] = Integer.toString(contentType);
		}

	
		public boolean getCanCut() {
			
			return canCut;
		}

}