package com.orange.game.dice.robot.client;

import java.util.logging.Logger;
import org.apache.commons.lang.math.RandomUtils;

public class DiceRobotIntelligence {
	
	private static final Logger logger = Logger.getLogger(DiceRobotIntelligence.class
			.getName());
		/*
		 *  定义一个多项式分布概率数组, 每一项表示5个骰子出现
		 *  x个y的概率(忽略了机器人自己的5个骰子).y可以是
		 *  1,2,3,4,5,6.   
		 *    
		 *  每一项由下列公式给出:
		 *    {n!/(x!(n-x)!)} * p^x * (1-p)^(n-x)
         *   其中,n表示5. x表示出现x个1(或2,或3,或4,或5,或6)
         *   的个数, p表示每个骰子出现1(或2,或3,或4,或5,或6)的概率,
         *   它的值是1/6.
		 */
		private final  double[] probablity =
			{
				0.401877572,   	// x takes 0
				0.401877572,   	// x takes 1
				0.160751029,   	// x takes 2
				0.0321502058,		// x takes 3
				0.00321502058,   	// x takes 4
				0.000128600823,   // x takes 5
			};
		private static final double BENCHMARK_PROB = 0.16;
		/*
		 * 5个骰子中出现1(或2,...或6)的个数的期望值M,
		 * 		期望值公式: M = ∑ k*pk
		 *  	k表示出现0次(1次,2次.....25次)
		 * 	pk是出现k次的对应概率,即上面的probablity数组
		 * 		∑是连加.
		 *   6项(5个骰子)相加算出来是3.302, 四舍五入成3.
		 */
		private  static final  int  DiceMeanValue = 3;
		
		
		// Robot's highest intelligence
		private static final double HIGHEST_IQ = 1;
		
		
		// IQ threshold affects how robot make decision.
		private static final double IQ_THRESHOLD = 0.6d;

		// A accelerator fator that control how
		//  fast the intelligence changes.
		private static final double ACCELERATOR_FACTOR = Math.E;
		
//      NOT USED NOW !!!
//		// Per-player's initial honesty
//		private static final int INIHONESTY = 10;
//		// At most five player(except robot itself) 
//		private int[] honesty = {INIHONESTY,INIHONESTY,INIHONESTY,INIHONESTY,INIHONESTY};
		
		// Robot's IQ.
		private  double intelligence;
		// Current round's benchmark probablity
		private double benchmark;

		// How many rounds the robot wins
		private int winRound;
		// How many rounds the robot loses
		private int loseRound;

		// A flag to indicate whether giving up calling.
		private boolean giveUpCalling = false;
		
		// How many rounds is this game in ?
		private int round = 0;
		
		// An array where we put what to call.
		// index 0: the number of dices
		// index 1: which dice
		// index 2: is wild ?
		private int[] whatToCall = {0, 0, 0};
		
		// What player last call
		private int last = 0;
		
		// Does player change?
		private boolean changed = false;
		
		 /* Introspection of robot's dices, set by inspectRobotDices method.
		  * index 0: any dice of 4/5 instances ? 1 for yes, 0 for no.
		  * index 1: which dice has 4/5 instances ? Only set if index 1 is 1.
		  * index 2: any dice of 3 instances ? 1 for yes, 0 for no.
		  * index 3: which dice has 3 instances ? Only set if index 3 is 1.
		  * index 4: any dice of 2 instances ? 1 for yes, 0 for no.
		  * index 5: which dice has 3 instances ? Only set if index 5 is 1.
		  * index 6: same as index 6(There may be two dices of 2 instances.
		  * index 7: distributed uniformly.  
		   */
		private int[] introspection = {0, 0, 0, 0, 0, 0, 0, 0};
		
		// How dose robot's dices distribute?
		private int[] distribution = {0,0,0,0,0,0};
		
		
		private boolean safe = true;
		private boolean lying = false;
		private int lieDice = 0;
		
		private void reset(int[] array) {
			for ( int i = 0; i< array.length; i++) {
				array[i] = 0 ;
			}
		}
		
		public DiceRobotIntelligence(int playUserCount) {
				intelligence = HIGHEST_IQ;
				benchmark = BENCHMARK_PROB / intelligence;
				winRound = 0;
				loseRound = 0;
		}
		
		public void balanceAndReset(boolean robotWinThisGame) {
			
			if ( robotWinThisGame ) {
				winRound++;
				intelligence = HIGHEST_IQ / Math.pow(ACCELERATOR_FACTOR, winRound);
				benchmark = (benchmark / intelligence > 0.5 ? 0.5 : benchmark / intelligence);
				loseRound = 0;
			} else {
				loseRound++;
				if ( intelligence * Math.pow(ACCELERATOR_FACTOR/2, loseRound) > HIGHEST_IQ)
					intelligence = HIGHEST_IQ;
				else 
					intelligence *= Math.pow(ACCELERATOR_FACTOR/2, loseRound);
				benchmark = (benchmark / intelligence > 0.5 ? 0.5 : benchmark / intelligence);
				winRound = 0;
			}
			round = 0;
			giveUpCalling = false;
			last = 0;
			changed = false;
			safe = true;
			lying = false;
			lieDice = 0;
			reset(whatToCall);
			reset(introspection);
			reset(distribution);
		}
		
		 
		public boolean canOpenDice(int playerCount,int seatId, int num, int dice, boolean isWild) {
			
			boolean canOpen = false;
			
			int notWild = (isWild == false? 1 : 0);
			// How many "dice" robot have.
			int numOfDice = distribution[dice-1] + distribution[0] * notWild;

			if ( dice != last && last != 0) {
				changed = true;
			} else {
				changed = false;
			}

			// Make a decision...
			if ( intelligence < IQ_THRESHOLD ) {
				canOpen = ((int)HIGHEST_IQ/intelligence >= 2 && num - numOfDice > 2 ? true : false);
				if(canOpen)
					logger.info("robot is not smart, it decides to open!");
				return canOpen;
			}
			
			if ( lying && dice == lieDice && num > 2) {
					canOpen = RandomUtils.nextInt(2) == 1? true : false;
					logger.info("Robot is lyint and player is fooled,open!");
					return canOpen;
			}
			
			if ( num - numOfDice > 0 ) {
				if ( num - numOfDice > 5 ) {
					canOpen = true;
					logger.info("Call to much, open!");
				}
				// Distributed uniformly & num is too big, it's not safe to call.
				else if ( introspection[7] == 1 && num - numOfDice >= 3) {
					canOpen = true;
					logger.info("Distributed uniformly & call too much, open!");
				}
				else if ( probablity[num -numOfDice] < benchmark ) {
					if ( round <= 2 ){
						canOpen = ( num - numOfDice > 3 ?  true : false );
						if (canOpen)
							logger.info("round <=2, call too much, open!");
					}
					if (round == 2 || round == 3) {
						if ( changed ) {
							canOpen = (round + RandomUtils.nextInt(2) > 2 ? true : false);
							if(canOpen)
								logger.info("round 2 or round 3, player changes calling  dice, he may be cheating, open!");
						}
						if ( num - numOfDice >= 3 ) {
							canOpen = (round + RandomUtils.nextInt(2) > 2 ? true : false);
							if(canOpen)
								logger.info("round 2 or round 3, call too much, open!");
						}
						else if ( !safe ){
							canOpen = (RandomUtils.nextInt(2) == 1 ? true : false );
							if(canOpen)
								logger.info("Not safe, open!");
						}
					}
					else if ( round > 4) {
						canOpen = true;
						logger.info("Too much round, calling is dangerous, open!");
					}
				}	
			}
			
			return canOpen;
		}
		
	public void decideWhatToCall(int playerCount,int num, int dice, boolean isWild, int[] robotDices) {
			
			int tmp = 0;
			
			giveUpCalling = false;
			whatToCall[0] = 0;
			whatToCall[1] = 0;
			whatToCall[2] = 0;
			
			
			// Just adding one even exceeds the limit, we should not call. 
			if ( num + 1 > 2*5 ) {
				giveUpCalling = true;
				return ;
			}
			
			logger.info("Now the IQ is " +intelligence);
			logger.info("Now the benchmark is " + benchmark);
			logger.info("Current round is Rnd: " + (round +1) );

			// We are first to call 
			if ( num == -1 || dice == -1) {
				intialCall(dice);
				return;
			}
			
			int notWild = (isWild == false? 1 : 0);
			// How many "dice" robot have.
			int numOfDice = distribution[dice-1] + distribution[0] * notWild;
			
			// Make decision...
			if (isWild){
				// Not so intelligent, just add 1
				if ( intelligence < IQ_THRESHOLD ){
					recordCall(num+1, dice,1);
					safe = false;
					logger.info("<DiceRobotIntelligence> isWild & not so smart, just add one, call "
							+ whatToCall[0]  + " X " + whatToCall[1] );
				}
				// Quite smart, do some deep thought.
				else {
					// We don't have as many dices as called.
					if ( num - numOfDice > 0 ) {
						for( int i= 0; i<distribution.length; i++ ) {
							if ( i + 1 > dice && distribution[i]>= num -numOfDice ) {
								recordCall(num, i+1, 1);
								round++;
								logger.info("<DiceRobotIntelligence> isWild & smart, change dice, call"
										+ whatToCall[0]  + " X " + whatToCall[1] );
								return;
							}
							else if ( i+1 <= dice && distribution[i] > num -numOfDice){
								recordCall(num + 1, i+1, 1);
								round++;
								logger.info("<DiceRobotIntelligence> isWild & smart, change dice, call"
										+ whatToCall[0]  + " X " + whatToCall[1] );
								return;
							}
						}
						// round+1 is current round, if current round is 3, it must be true,
						// if current round is 2, it is 50% true. 
						if ( round == 0 || round + 1 + RandomUtils.nextInt(2) > 2 ){
							recordCall(num+1, dice,1);
							safe = false;
							logger.info("<DiceRobotIntelligence> isWild & smart, just add one, call "
									+ whatToCall[0]  + " X " + whatToCall[1] );
						} else {
							logger.info("<DiceRobotIntelligence> isWild &  smart, but not safe to call, give up calling");
							giveUpCalling = true;
							round++;
							return;
						}
					} 
					// We have more dices than that called.
					else {
						// Some dice has more than 4 instances...
						if ( introspection[0] == 1 ) {
							recordCall((introspection[1] > dice ? num : num +1), introspection[1], 1);
								logger.info("<DiceRobotIntelligence> isWild & smart, "+introspection[1] 
										+ "has more than 4 instances, so change dice to " + introspection[1]+
										", call "+ whatToCall[0] + " X " + whatToCall[1]);
								// Some dice has 3 instances...
						} else if ( introspection[2] == 1 ) {
							recordCall((introspection[3] > dice ? num : num +1), introspection[3], 1);
							logger.info("<DiceRobotIntelligence> isWild & smart, "+introspection[3] 
									+ "has 3 instances, so change dice to " + introspection[3]+
									", call "+ whatToCall[0] + " X " + whatToCall[1]);
						}
						else {
							recordCall(num+1, dice, 1);
							safe = false;
							logger.info("<DiceRobotIntelligence> isWild &  smart, just add one, call "
									+ whatToCall[0]  + " X " + whatToCall[1] );
						}
					}
				}
			} // end of if(isWilds) 
			// Not wild~
			else {
				// Not so intelligent, rudely call...
				if ( intelligence < IQ_THRESHOLD ){
					recordCall((num + 1+ RandomUtils.nextInt(2)), dice, 0);
					safe = false;
					logger.info("<DiceRobotIntelligence> Not Wild & not so smart, rudely call, call "
							+ whatToCall[0]  + " X " + whatToCall[1]);
				}
				// Smart, quite lots of choice.
				else {
					// Do we have many ONEs?
					if ( distribution[0] >= 3 ){
							// YES, call ONE(auto wild)
							if ( num <= DiceMeanValue + distribution[0] ) {
								recordCall(num, 1, 1);
								safe = false;
								logger.info("<DiceRobotIntelligence> Not Wild & smart, many ONEs, call one! Call "
										+ whatToCall[0]  + " X " + dice );
							} else {
								// YES, but the num is some little big, call wilds is not safe,
								// we should be careful.
								if ( introspection[4] == 1) {
									recordCall((dice > introspection[5] ? num+1 : num), introspection[5], 0);
									safe = false;
 									logger.info("<DiceRobotIntelligence> Not Wild &  smart, have many ONEs & has dice of 2 instances, call "
 											+ whatToCall[0]  + " X " + whatToCall[1]);
								}
								else {
									tmp = probablity[num + 2] > benchmark? num + 2 : num + 1;
									recordCall(tmp, dice, 0);
									logger.info("<DiceRobotIntelligence> Not Wild & smart, many ONEs, but not safe to call wilds, call "
											+ whatToCall[0]  + " X " + dice );
								}
							}
						
					} 
					// We have any dice of more than 4 instances, change dice...
					else if ( introspection[0] == 1 ) {
						recordCall((dice >= introspection[1] ? num + 1 : num ), introspection[1], 0);
						logger.info("<DiceRobotIntelligence> Not Wild &  smart, has more than 4 "+introspection[1]+
								", so change dice to " + introspection[1]+", call "+ whatToCall[0] + " X " + whatToCall[1]);
					}
					// We have dice of 3 instances...
					else if ( introspection[2] == 1 && distribution[0] == 2) {
							recordCall(num + 1, introspection[3], 0);
							logger.info("<DiceRobotIntelligence> Not Wild &  smart, has 3 "+introspection[3]+ " & 2 ONEs, call "
									+ whatToCall[0]  + " X " + whatToCall[1]);
					}
					// We have dice of 2 intances...
					else if ( introspection[4] == 1) {
						if (introspection[5] != 1 && distribution[0] > 1 ) {
						recordCall(num + 1, introspection[5], 0);
						logger.info("<DiceRobotIntelligence> Not Wild & smart, has 2 X "+introspection[5]+" & more than 1 ONE, call "
								+ whatToCall[0]  + " X " + whatToCall[1]);
						}
						else if ( dice == introspection[5] ) {
							 recordCall(num + 1, introspection[5], 0);
							 logger.info("<DiceRobotIntelligence> Not Wild & smart, has 2 X " + introspection[5]+ ", call "
									 + whatToCall[0]  + " X " + whatToCall[1]);
						 } 
						else if ( dice == introspection[6] ) {
							 recordCall(num + 1, introspection[6], 0);
							 logger.info("<DiceRobotIntelligence> Not Wild & smart, has 2 X " + introspection[6]+ ", call "
									 + whatToCall[0]  + " X " + whatToCall[1]);
						 }
						 else {
							 recordCall((dice >= introspection[5]? num+1:num) , introspection[5], 0);
							 logger.info("<DiceRobotIntelligence> Not Wild & smart, has 2 X " + introspection[5]+ ", call "
									 + whatToCall[0]  + " X " + whatToCall[1]);
						 }
					}
					// We have our dices distributed uniformly,do a safe call.
					else {
						recordCall(num + 1, dice, 0);
						logger.info("<DiceRobotIntelligence> Not Wild & smart,dices distributed uniformly, just do a safe call , call "
								+ whatToCall[0]  + " X " + whatToCall[1] );
					}
				} // end of smart
			} // end of not Wild
			
			round++;
		}

	private void recordCall(int num, int dice, int isWild) {
		whatToCall[0] = num;
		whatToCall[1] = dice;
		whatToCall[2] = isWild;
	}

	// Robot initiate the call.
	private void intialCall(int dice) {
		
		if ( intelligence < IQ_THRESHOLD) {
			recordCall(1 + RandomUtils.nextInt(3), 1+RandomUtils.nextInt(6), RandomUtils.nextInt(2) );
			safe = false;
			logger.info("<intialCall> Initially, not smart, just do a random call , call "
					+ whatToCall[0]  + " X " + whatToCall[1] );
			if ( distribution[whatToCall[1]-1] == 0 ) {
				lying  = true;
				lieDice  = whatToCall[1];
			}
		}
		// Smart...
		else {
			if ( distribution[0] >= 3 ){
				if ( RandomUtils.nextInt(2) == 1 ) {
					recordCall(distribution[0], 1, 1);
					safe = false;
				} else {
					recordCall(distribution[0], RandomUtils.nextInt(5)+2, 0);
					safe = false;
				} 
				logger.info("<intialCall> Initially,smart, has more than 3 ONEs,just call ONE or do a random call , call "
						+ whatToCall[0]  + " X " + whatToCall[1] );
			}
			else if ( introspection[0] == 1 ) {
				recordCall(distribution[introspection[1]-1], introspection[1], 0);
				logger.info("<intialCall> Initial call,has more than 4 " + introspection[1]+
						", so call "+ whatToCall[0] + " X " + whatToCall[1]);
			}
			else if ( introspection[2] == 1) {
				recordCall(3, introspection[3], RandomUtils.nextInt(2));
				logger.info("<intialCall> Initial call,has 3 " + introspection[3]+
						", so call "+ whatToCall[0] + " X " + whatToCall[1]);
			}
			else if ( introspection[4] == 1 ){
				if ( introspection[6] != 0 ) {
					recordCall(2 + (introspection[6] != 1 ? distribution[0] :0), introspection[6], 0);
				}
				else {
					recordCall(2 + (introspection[5] != 1 ? distribution[0] :0), introspection[5], 0);
				}
				logger.info("<intialCall> Initial call,has dice of 2 instances, so call "
						+ whatToCall[0] + " X " + whatToCall[1]);
			}
			else {
				recordCall(1 + RandomUtils.nextInt(3), 2+RandomUtils.nextInt(5), RandomUtils.nextInt(2) );
				safe = false;
				logger.info("<intialCall> Initially, smart,dices distributed uniformly,  do a random call , call "
						+ whatToCall[0]  + " X " + whatToCall[1]);
				if ( distribution[whatToCall[1]-1] == 0 ) {
					lying  = true;
					lieDice  = whatToCall[1];
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
			
			result[0] = whatToCall[0];
			result[1] = whatToCall[1];
			result[2] = whatToCall[2];
			
			return result;
		}
		
		
		public void inspectRobotDices(int[] robotDices) {
			
			logger.info("robotDices[] is: "+ robotDices[0]+", " +robotDices[1]
					+", " + robotDices[2]+", "+ robotDices[3]+", " +
					robotDices[4]);
			
			for ( int i= 0; i < robotDices.length;i++ ) {
				switch (robotDices[i]) {
				case 1:
					distribution[0]++;
					break;
				case 2:
					distribution[1]++;
					break;
				case 3:
					distribution[2]++;
					break;
				case 4:
					distribution[3]++;
					break;
				case 5:
					distribution[4]++;
					break;
				case 6:
					distribution[5]++;
					break;
				default:
					break;
				}
				
			}
			for (int i = 0;i < distribution.length; i++) {
				if ( distribution[i] > 3 ) {
					introspection[0] = 1;
					introspection[1] = i+1;
				} 
				else if ( distribution[i] == 3 ) {
					introspection[2] = 1;
					introspection[3] = i+1;
				}
				else if ( distribution[i] == 2 ) {
					introspection[4] = 1;
					if ( introspection[5] == 0 ) {
						introspection[5] = i+1;
					}
					else {
						introspection[6] = i+1;
					}
				}
				
			}// end of for
			
			if ( introspection[0] == 0 && introspection[2] == 0 && introspection[4] == 0) {
					introspection[7] = 1;
			}
		}
}