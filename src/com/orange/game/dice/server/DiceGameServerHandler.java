package com.orange.game.dice.server;

import org.jboss.netty.channel.MessageEvent;

import com.orange.common.log.ServerLog;
import com.orange.game.dice.messagehandler.CallDiceRequestHandler;
import com.orange.game.dice.messagehandler.DiceJoinGameRequestHandler;
import com.orange.game.dice.messagehandler.OpenDiceRequestHandler;
import com.orange.game.dice.messagehandler.UseItemRequestHandler;
import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.messagehandler.ChatRequestHandler;
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
import com.orange.game.traffic.service.SessionUserService;
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
				
			case CHAT_REQUEST:
				return new ChatRequestHandler(messageEvent);

			case JOIN_GAME_REQUEST:
				return new DiceJoinGameRequestHandler(messageEvent);
				
			case CALL_DICE_REQUEST:
				return new CallDiceRequestHandler(messageEvent);

			case OPEN_DICE_REQUEST:
				return new OpenDiceRequestHandler(messageEvent);
				
			case REGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new RegisterRoomsRequestHandler(messageEvent);
				
			case UNREGISTER_ROOMS_NOTIFICATION_REQUEST:
				return new UnRegisterRoomsRequestHandler(messageEvent);
				
			case USE_ITEM_REQUEST:
				return new UseItemRequestHandler(messageEvent);
		}
		
		return null;
	}

	@Override
	public void userQuitSession(String userId,
			GameSession session, boolean needFireEvent) {
		
		GameEventExecutor.getInstance().getSessionManager().userQuitSession(session, userId, needFireEvent);
				
		
		
	}
	
}
