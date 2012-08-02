package com.orange.game.dice.server;

import java.nio.channels.ClosedChannelException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.messagehandler.CallDiceRequestHandler;
import com.orange.game.dice.messagehandler.OpenDiceRequestHandler;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.GameServerHandler;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class DiceGameServerHandler extends GameServerHandler {
	private static final Logger logger = Logger.getLogger(DiceGameServerHandler.class.getName());

	@Override
	public AbstractMessageHandler getMessageHandler(MessageEvent messageEvent) {
		
		GameMessage message = (GameMessage)messageEvent.getMessage();
		
		switch (message.getCommand()){
		case JOIN_GAME_REQUEST:
			return new JoinGameRequestHandler(messageEvent);
			
		case CALL_DICE_REQUEST:
			return new CallDiceRequestHandler(messageEvent);
			
		case OPEN_DICE_REQUEST:
			return new OpenDiceRequestHandler(messageEvent);
		}
		
		return null;
	}

	@Override
	public void userQuitSession(String userId,
			GameSession session, boolean needFireEvent) {
				
		int sessionId = session.getSessionId();
		ServerLog.info(sessionId, "user "+userId+" quit");

		session.takeOverUser(userId);
		
		GameCommandType command = null;		
		if (session.isCurrentPlayUser(userId)){
			command = GameCommandType.LOCAL_PLAY_USER_QUIT;			
//			session.setCompleteReason(GameCompleteReason.REASON_DRAW_USER_QUIT);			
		}
		else if (session.getUserCount() <= 2){
			command = GameCommandType.LOCAL_ALL_OTHER_USER_QUIT;			
//			session.setCompleteReason(GameCompleteReason.REASON_ONLY_ONE_USER);			
		}
		else {
			command = GameCommandType.LOCAL_OTHER_USER_QUIT;						
//			updateCurrentPlayer(session);			
		}			
		
		// broadcast user exit message to all other users
		NotificationUtils.broadcastUserStatusChangeNotification(session, userId);			
		
		// fire message
		if (needFireEvent){
			GameEventExecutor.getInstance().fireAndDispatchEvent(command, sessionId, userId);
		}
		
	}
	
}
