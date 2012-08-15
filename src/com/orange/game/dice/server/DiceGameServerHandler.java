package com.orange.game.dice.server;

import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.messagehandler.CallDiceRequestHandler;
import com.orange.game.dice.messagehandler.OpenDiceRequestHandler;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.messagehandler.room.CreateRoomRequestHandler;
import com.orange.game.traffic.messagehandler.room.GetRoomRequestHandler;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.messagehandler.room.RegisterRoomsRequestHandler;
import com.orange.game.traffic.messagehandler.room.UnRegisterRoomsRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.model.dao.GameUser;
import com.orange.game.traffic.server.GameEventExecutor;
import com.orange.game.traffic.server.GameServerHandler;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class DiceGameServerHandler extends GameServerHandler {
//	private static final Logger logger = Logger.getLogger(DiceGameServerHandler.class.getName());

	@Override
	public AbstractMessageHandler getMessageHandler(MessageEvent messageEvent) {
		
		GameMessage message = (GameMessage)messageEvent.getMessage();
		
		switch (message.getCommand()){
			case CREATE_ROOM_REQUEST:
				return new CreateRoomRequestHandler(messageEvent);
				
			case GET_ROOMS_REQUEST:
				return new GetRoomRequestHandler(messageEvent);
				
				
			case JOIN_GAME_REQUEST:
				return new JoinGameRequestHandler(messageEvent);
				
			case CALL_DICE_REQUEST:
				return new CallDiceRequestHandler(messageEvent);

			case OPEN_DICE_REQUEST:
				return new OpenDiceRequestHandler(messageEvent);
				
			case REGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new RegisterRoomsRequestHandler(messageEvent);
				
			case UNREGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new UnRegisterRoomsRequestHandler(messageEvent);
		}
		
		return null;
	}

	@Override
	public void userQuitSession(String userId,
			GameSession session, boolean needFireEvent) {
				
		int sessionId = session.getSessionId();
		ServerLog.info(sessionId, "user "+userId+" quit");

		GameUser user = session.findUser(userId);
		boolean removeUser = (user.isPlaying() == false);
		
		if (!removeUser){
			session.takeOverUser(userId);
		}
		
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
		
		if (removeUser){
			GameEventExecutor.getInstance().removeUser(session, userId);
		}
		
		
	}
	
}
