package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.traffic.messagehandler.AbstractMessageHandler;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;

public class CallDiceRequestHandler extends AbstractMessageHandler {

	public CallDiceRequestHandler(MessageEvent messageEvent) {
		super(messageEvent);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(GameMessage message, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isProcessForSessionAllocation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessIgnoreSession() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProcessInStateMachine() {
		// TODO Auto-generated method stub
		return false;
	}

}
