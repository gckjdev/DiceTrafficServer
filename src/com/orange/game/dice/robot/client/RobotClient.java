package com.orange.game.dice.robot.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.BasicDBObject;
import com.orange.common.log.ServerLog;
import com.orange.common.utils.RandomUtil;
import com.orange.game.constants.DBConstants;
import com.orange.game.dice.db.DrawDBClient;
import com.orange.game.dice.robot.manager.RobotManager;
import com.orange.game.dice.server.DiceGameServer;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameServer;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.GeneralNotification;
import com.orange.network.game.protocol.message.GameMessageProtos.JoinGameRequest;
import com.orange.network.game.protocol.message.GameMessageProtos.SendDrawDataRequest;
import com.orange.network.game.protocol.model.GameBasicProtos.PBDraw;
import com.orange.network.game.protocol.model.GameBasicProtos.PBDrawAction;
import com.orange.network.game.protocol.model.GameBasicProtos.PBGameUser;

public class RobotClient implements Runnable {

	private static final int MIN_SESSION_USER_COUNT = 2;
	final int sessionId;
	final String userId;
	final String nickName;
	final boolean gender;
	final String userAvatar;
	final String location;
	final int robotIndex;
	
	enum ClientState{
		WAITING,
		PICK_WORD,
		PLAYING
	};
	
	// game session running data
	ConcurrentHashMap<String, GameUser> userList = new ConcurrentHashMap<String, GameUser>();
	ClientState state = ClientState.WAITING;	
	String currentPlayUserId = null;
	String friendRoomId = null;
	int round = -1;
	String word = null;
	int level = 0;
	int language = 0;
	int guessCount = 0;
	boolean guessCorrect = false;
	
	// simulation
	Timer guessWordTimer;
	PBDraw pbDraw;
	
	// connection information
	ChannelFuture future;
	ClientBootstrap bootstrap;
	Channel channel;

	// message
	AtomicInteger messageId = new AtomicInteger(1);
	
	public RobotClient(String userId, String nickName, String avatar, boolean gender,
			String location,
			int sessionId, int index){
		this.sessionId = sessionId;
		this.location = location;
		this.userAvatar = avatar;
		this.userId = userId;
		this.gender = gender;
		this.nickName = nickName;
		this.robotIndex = index;
	}
			
	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public void run(){
        
        String host = "127.0.0.1";
        int port = GameServer.getPort();
        int THREAD_NUM = 1;

        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newFixedThreadPool(THREAD_NUM),
                        Executors.newFixedThreadPool(THREAD_NUM)));
        
        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new RobotClientPipelineFactory(this));

        // Start the connection attempt.
        future = bootstrap.connect(new InetSocketAddress(host, port));
        
        // Wait until the connection is closed or the connection attempt fails.
        future.getChannel().getCloseFuture().awaitUninterruptibly();
        future = null;
        
        // Shut down thread pools to exit.
        bootstrap.releaseExternalResources();		        
        bootstrap = null;        
	}
	
	void send(GameMessage msg){
		if (channel != null && channel.isWritable()){
			ServerLog.info(sessionId, "robot "+nickName+ " send "+msg.getCommand());
			channel.write(msg);
		}
	}
	
	void sendJoinGameRequest() {

		JoinGameRequest request = null;
		GameMessage message = null;
			
		if (friendRoomId == null){
			request = JoinGameRequest.newBuilder().setUserId(userId)
					.setNickName(nickName)
					.setGender(gender).
					setAvatar(userAvatar)
					.setLocation(location)
					.setIsRobot(true)
					.setTargetSessionId(sessionId)
					.setGameId("")
					.build();
		}
		else{
			request = JoinGameRequest.newBuilder().setUserId(userId)
			.setNickName(nickName)
			.setGender(gender).
			setAvatar(userAvatar)
			.setLocation(location)
			.setIsRobot(true)
			.setTargetSessionId(sessionId)
			.setGameId("")
			.setRoomId(friendRoomId)
			.build();			
		}
				
		message = GameMessage.newBuilder().setMessageId(messageId.getAndIncrement())
				.setCommand(GameCommandType.JOIN_GAME_REQUEST)
				.setJoinGameRequest(request).build();
		
		send(message);
	}

	public synchronized void disconnect() {
		ServerLog.info(sessionId, "Robot " + nickName + " Disconnect");
		
		this.resetPlayData();
		
		if (channel != null){
			if (channel.isConnected()){
				channel.disconnect();
			}

			channel.close();
			channel = null;
		}
	}
		
	public void removeUserByUserId(String userIdForRemove) {
		userList.remove(userIdForRemove);
	}

	/*
	public int sessionUserCount() {		
		return userList.size();
	}
	*/

	public void sendQuitGameRequest() {
		disconnect();
	}

	public int getClientIndex() {
		return robotIndex;
	}

	public void stopClient() {
		
		disconnect();
		
		if (future != null){		
			future.cancel();
		}
		
        // Shut down thread pools to exit.
		if (bootstrap != null){
			bootstrap.releaseExternalResources();
		}
	}

	public ClientState getState() {
		return state;
	}

	public void setState(ClientState state) {
		this.state = state;
	}

	public void updateByNotification(GeneralNotification notification) {
		
		if (notification == null){
			return;
		}
		
		if (notification.hasCurrentPlayUserId()){
			this.setCurrentPlayUserId(notification.getCurrentPlayUserId());			
		}
		
		if (notification.hasNewUserId()){
			// rem by Benson for new framework 
//			this.addNewUser(notification.getNewUserId(),
//					notification.getNickName(),
//					notification.getUserAvatar(),
//					notification.getUserGender());
		}
		
		if (notification.hasQuitUserId()){
			removeUserByUserId(notification.getQuitUserId());
		}
		
	}

//	private void addNewUser(String newUserId, String nickName2,
//			String userAvatar2, boolean userGender) {
//
//		if (newUserId == null){
//			return;
//		}
//		
//		GameUser user = new GameUser(newUserId, nickName2, userAvatar2, userGender, 
//				"New York, USA", null, null, sessionId, 1, 5);
//		userList.put(newUserId, user);
//	}

	private void setCurrentPlayUserId(String userId) {
		this.currentPlayUserId = userId;
	}

	public boolean canQuitNow() {
		if (this.sessionRealUserCount() >= MIN_SESSION_USER_COUNT){
			return true;
		}
		else{
			return false;
		}
	}

	public int sessionRealUserCount() {
		Collection<GameUser> list = userList.values();
		int userCount = 0;
		for (GameUser user : list){
			if (!RobotManager.isRobotUser(user.getUserId())){
				userCount ++;
			}
		}
		return userCount;
	}

	public void updateTurnData(GeneralNotification notification) {
		if (notification == null)
			return;
		
		if (notification.hasRound())
			this.round = notification.getRound();
		
		if (notification.hasWord() && notification.getWord().length() > 0)
			this.word = notification.getWord();
		
		if (notification.hasLevel())
			this.level = notification.getLevel();
		
		if (notification.hasLanguage() && notification.getLanguage() > 0)
			this.language = notification.getLanguage();
		
	}
	
	public final void sendGuessWord(String guessWord){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder().setGuessWord(guessWord)
			.setGuessUserId(userId)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(messageId.getAndIncrement())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}
	
	public final void sendStartGame(){		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.START_GAME_REQUEST)
			.setMessageId(messageId.getAndIncrement())
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);
	}

	public final void cleanDraw(){		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.CLEAN_DRAW_REQUEST)
			.setMessageId(messageId.getAndIncrement())
			.setUserId(userId)
			.setSessionId(sessionId)
			.build();
		
		send(message);
	}
	
	public final void sendStartDraw(String word, int level, int language){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder().setWord(word)
			.setLevel(level)
			.setLanguage(language)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(messageId.getAndIncrement())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}

	public final void sendDraw(List<Integer> pointList, float width, int color){
		SendDrawDataRequest request = SendDrawDataRequest.newBuilder()
			.addAllPoints(pointList)
			.setWidth(width)
			.setColor(color)
			.build();
		
		GameMessage message = GameMessage.newBuilder().setCommand(GameCommandType.SEND_DRAW_DATA_REQUEST)
			.setMessageId(messageId.getAndIncrement())
			.setUserId(userId)
			.setSessionId(sessionId)
			.setSendDrawDataRequest(request)
			.build();
		
		send(message);
	}	
	
	public void setGuessWordTimer(){
				
		clearGuessWordTimer();				
		
		if (currentPlayUserId.equals(userId)){
			// draw user cannot guess...
			return;
		}
		
		guessWordTimer = new Timer();
		guessWordTimer.schedule(new TimerTask(){

			@Override
			public void run() {	
				try{
					
					if (guessCorrect){
						ServerLog.info(sessionId, "Robot client, try guess but already guess correct");
						return;
					}
					
					String guessWord = null;
//					boolean isMatchWordLen = (language == DrawGameServer.LANGUAGE_CHINESE) ? false : true;
					
										
					boolean isMatchWordLen = true;
					String randomWord = null;
					
					if (guessCount >= 3 && RandomUtil.random(1) == 0){
						guessWord = word;
					}
					else{
//						randomWord = DrawStorageService.getInstance().randomGetWord(language, word);
//						if (randomWord == null){
//							randomWord = WordManager.getInstance().randomGetWord(language, word.length(), isMatchWordLen);
//						}

						guessWord = randomWord;
					}
					
					if (guessWord.equalsIgnoreCase(word)){
						guessCorrect = true;
					}
					
					guessCount ++;
					sendGuessWord(guessWord.toUpperCase());
					
				}
				catch (Exception e){
					ServerLog.error(sessionId, e, "robot client guess word timer ");
				}

				// schedule next timer
				if (!guessCorrect){
					setGuessWordTimer();
				}
			}
			
		}, 1000*RandomUtil.random(RANDOM_GUESS_WORD_INTERVAL)+1000);
	}
	
	public void clearGuessWordTimer(){
		if (guessWordTimer != null){
			guessWordTimer.cancel();
			guessWordTimer = null;
		}
		
	}

	public void resetPlayData() {
		clearGuessWordTimer();
		clearStartGameTimer();
		clearStartDrawTimer();
		
		guessCount = 0;
		guessCorrect = false;
	}

	public void saveUserList(List<PBGameUser> pbUserList) {
		if (pbUserList == null)
			return;
		
		userList.clear();
		for (PBGameUser pbUser : pbUserList){
			GameUser user = new GameUser(pbUser,
					null, sessionId);
			userList.put(pbUser.getUserId(), user);
		}
	}

	Timer startGameTimer = null;
	Timer startDrawTimer = null;
	int sendDrawIndex = 0;
	private static final int START_TIMER_WAITING_INTERVAL = 10;
	private static final int START_DRAW_WAITING_INTERVAL = 1;
	public static int RANDOM_GUESS_WORD_INTERVAL = 20;	
	
	public void clearStartDrawTimer(){
		sendDrawIndex = 0;

		if (startDrawTimer != null){
			startDrawTimer.cancel();
			startDrawTimer = null;
		}		
	}
	
	public void clearStartGameTimer(){
		if (startGameTimer != null){
			startGameTimer.cancel();
			startGameTimer = null;
		}
	}
	
	public void checkStart() {
		if (state != ClientState.WAITING)
			return;
		
		if (!this.currentPlayUserId.equals(this.userId)){
			return;
		}
		
		if (startGameTimer != null){
			// ongoing...
			return;
		}
		
		resetPlayData();
		
		startGameTimer = new Timer();
		startGameTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				
				try{
				
					Set<String> excludeUserSet = new HashSet<String>();
					Set<String> userIdList = userList.keySet();
					userIdList.remove(userId);
					for (String id : userIdList){
						if (!RobotManager.isRobotUser(id)){
							excludeUserSet.add(id);						
						}
					}
					
					BasicDBObject obj = null; // DrawStorageService.getInstance().randomGetDraw(sessionId,
//							0,
//							excludeUserSet);
					if (obj == null){
						ServerLog.warn(sessionId, "robot cannot find any draw for simulation! have to quit");
						disconnect();
						return;					
					}
	
					byte[] data = (byte[])obj.get(DBConstants.F_DRAW_DATA);
					if (data == null){
						ServerLog.warn(sessionId, "robot cannot find any draw for simulation! have to quit");
						disconnect();
						return;					
					}
					
					try {
						pbDraw = PBDraw.parseFrom(data);
					} catch (InvalidProtocolBufferException e) {
						ServerLog.warn(sessionId, "robot catch exception while parsing draw data, e="+e.toString());
						disconnect();
						return;					
					}
					
					String word = obj.getString(DBConstants.F_DRAW_WORD);
					int level = obj.getInt(DBConstants.F_DRAW_LEVEL);
					int language = obj.getInt(DBConstants.F_DRAW_LANGUAGE);
					
					sendStartGame();
					sendStartDraw(word, level, language);				
	
					state = ClientState.PLAYING;				
					scheduleSendDrawDataTimer(pbDraw);	
				}
				catch(Exception e){
					ServerLog.error(sessionId, e);					
				}
			}


			
		}, RandomUtil.random(START_TIMER_WAITING_INTERVAL)*1000+3000);
		
	}
	
	private void scheduleSendDrawDataTimer(final PBDraw pbDraw) {
		if (state != ClientState.PLAYING){
			return;
		}
		
		if (!this.currentPlayUserId.equals(this.userId)){
			return;
		}
		
		// clear previous draw timer if exists
		clearStartDrawTimer();
		
		startDrawTimer = new Timer();
		startDrawTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				try{
					if (sendDrawIndex < 0 || sendDrawIndex >= pbDraw.getDrawDataCount()){
						ServerLog.info(sessionId, "robot has no more draw data");
						clearStartDrawTimer();
						return;
					}
					
					PBDrawAction drawData = pbDraw.getDrawData(sendDrawIndex);
//					if (drawData.getType() == DrawAction.DRAW_ACTION_TYPE_CLEAN)
//						cleanDraw();
//					else
//						sendDraw(drawData.getPointsList(), drawData.getWidth(), drawData.getColor());
					
					sendDrawIndex++;
				}
				catch(Exception e){
					ServerLog.error(sessionId, e);					
				}
			}
			
		}, START_DRAW_WAITING_INTERVAL*1000+1000, START_DRAW_WAITING_INTERVAL*1000+1000);
	}

	public void setRoomId(String roomId) {
		this.friendRoomId = roomId;
	}	
}
