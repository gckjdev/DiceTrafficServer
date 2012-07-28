package com.orange.game.dice.robot.client;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.robot.RobotService;
import com.orange.game.dice.robot.client.RobotClient.ClientState;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class RobotClientHandler extends SimpleChannelUpstreamHandler {

//	private static final Logger logger = Logger.getLogger(RobotClientHandler.class.getName());
	final RobotClient robotClient;

	public RobotClientHandler(RobotClient client) {
		this.robotClient = client;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		super.handleUpstream(ctx, e);
	}

	int randValue(int number){
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		return random.nextInt(number);
	}
	
	private void handleJoinGameResponse(GameMessage message){

		if (message.getResultCode() != GameResultCode.SUCCESS){
			ServerLog.warn(robotClient.sessionId, "robot JOIN GAME failure, error="+message.getResultCode());
			robotClient.disconnect();
			return;
		}		
		
		// save user data here
		if (message.getJoinGameResponse() == null)
			return;
		
		if (message.getJoinGameResponse().getGameSession() == null)
			return;
						
		robotClient.saveUserList(message.getJoinGameResponse().getGameSession().getUsersList());
		robotClient.currentPlayUserId = message.getJoinGameResponse().getGameSession().getCurrentPlayUserId();
		
		robotClient.checkStart();
	}
	
	
	private void handleQuitGameResponse(GameMessage message){
	}
	
	private void handleStartGameResponse(GameMessage message) {
//		service.sendStartDraw(user, "杯子", 1);
		if (message.getResultCode() != GameResultCode.SUCCESS){
			ServerLog.info(robotClient.sessionId, "start game but response code is "+message.getResultCode());
			robotClient.disconnect();
		}
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		GameMessage message = (GameMessage) e.getMessage();
		
		switch (message.getCommand()){
		
		case JOIN_GAME_RESPONSE:
			handleJoinGameResponse(message);
			break;
						
		case START_GAME_RESPONSE:
			break;
			
		case NEW_DRAW_DATA_NOTIFICATION_REQUEST:
			handleDrawDataNotification(message);
			break;
			
		case GAME_TURN_COMPLETE_NOTIFICATION_REQUEST:			
			handleGameTurnCompleteNotification(message);
			break;
			
		case GAME_START_NOTIFICATION_REQUEST:
			handleGameStartNotification(message);
			break;

		case USER_JOIN_NOTIFICATION_REQUEST:			
			handleUserJoinNotification(message);
			break;
			
		case USER_QUIT_NOTIFICATION_REQUEST:
			handleUserQuitNotification(message);
			break;
		}


	}

	private void handleGameTurnCompleteNotification(GameMessage message) {
		robotClient.setState(ClientState.WAITING);
		robotClient.updateByNotification(message.getNotification());		
		robotClient.resetPlayData();
		
		if (robotClient.canQuitNow()){
			ServerLog.info(robotClient.sessionId, "reach min user for session, robot can escape now!");
			robotClient.disconnect();
			return;
		}
		
		robotClient.checkStart();

	}

	private void handleDrawDataNotification(GameMessage message) {
		robotClient.updateTurnData(message.getNotification());
		
		if (message.getNotification() == null)
			return;
			
		String word = message.getNotification().getWord();
		if (word != null && word.length() > 0){
			robotClient.setState(ClientState.PLAYING);
			robotClient.resetPlayData();

			// now here need to simulate guess word...
			robotClient.setGuessWordTimer();
		}
		
	}

	private void handleGameStartNotification(GameMessage message) {
		if (robotClient.state != ClientState.PLAYING){
			robotClient.setState(ClientState.PICK_WORD);
		}
		robotClient.updateByNotification(message.getNotification());								
	}

	private void handleUserJoinNotification(GameMessage message) {
		
		robotClient.updateByNotification(message.getNotification());						
		robotClient.checkStart();		
	}

	private void handleUserQuitNotification(GameMessage message) {
		String userId = message.getNotification().getQuitUserId();
		if (userId == null){
			return;
		}
		
		robotClient.removeUserByUserId(userId);
		if (robotClient.sessionRealUserCount() <= 0){
			// no other users, quit robot
			robotClient.sendQuitGameRequest();
			RobotService.getInstance().finishRobot(robotClient);
		}
		
		robotClient.checkStart();		
	}

	Timer startTimer = null;

	private void startGame(long delay){
		
		if (startTimer != null){
			startTimer.cancel();
			startTimer = null;
		}
		
		startTimer = new Timer();
		startTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
//				logger.info("<Start> " + user.getNickName()+" start game, session in " + user.getSessionId());
//				service.sendStartRequst(user);
//				startGameByMe = true;
//				logger.info("<StartCount>:"+ (++startCount));					
			}
		},delay);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		ServerLog.info(robotClient.sessionId, "catch exception, cause="+e.getCause());
		e.getChannel().disconnect();
		e.getChannel().close();
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) {
		e.getChannel().close();
		
		ServerLog.info(robotClient.sessionId, "<robotClient> channel disonnected");
		RobotService.getInstance().finishRobot(robotClient);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		ServerLog.info(robotClient.sessionId, "<robotClient> channel connected");
		robotClient.setChannel(e.getChannel());		
		robotClient.sendJoinGameRequest();
	}
}
