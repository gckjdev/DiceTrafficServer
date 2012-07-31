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

import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.server.GameServerHandler;
import com.orange.network.game.protocol.message.GameMessageProtos;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.constants.GameConstantsProtos;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameCommandType;
import com.orange.network.game.protocol.constants.GameConstantsProtos.GameResultCode;



public class DiceGameServerHandler extends GameServerHandler {
	private static final Logger logger = Logger.getLogger(DiceGameServerHandler.class.getName());

	@Override
	public AbstractMessageHandler getMessageHandler(MessageEvent messageEvent) {
		
		GameMessage message = (GameMessage)messageEvent.getMessage();
		
		switch (message.getCommand()){
		case JOIN_GAME_REQUEST:
			return new JoinGameRequestHandler(messageEvent);
			
		case CALL_DICE_REQUEST:
			break;
			
		case OPEN_DICE_REQUEST:
			break;
		}
		
		return null;
	}

	@Override
	public void processDisconnectChannel(Channel channel,
			DisconnectReason reason) {
		// TODO Auto-generated method stub
		
	} 
	
}
