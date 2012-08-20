package com.orange.game.dice.messagehandler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.MessageEvent;

import com.orange.game.dice.model.DiceGameSession;
import com.orange.game.traffic.messagehandler.room.JoinGameRequestHandler;
import com.orange.game.traffic.model.dao.GameSession;
import com.orange.game.traffic.server.NotificationUtils;
import com.orange.network.game.protocol.message.GameMessageProtos.GameMessage;
import com.orange.network.game.protocol.message.GameMessageProtos.UserDiceNotification;

public class DiceJoinGameRequestHandler extends JoinGameRequestHandler {

	public DiceJoinGameRequestHandler(MessageEvent event) {
		super(event);
	}
	
	@Override
	public void handleRequest(GameMessage message, Channel channel, GameSession requestSession) {

		DiceGameSession session = (DiceGameSession)processRequest(message, channel, requestSession);
		
		// send user dice notification
		if (session.isGamePlaying()){
			UserDiceNotification diceNotification = UserDiceNotification.newBuilder()
				.addAllUserDice(session.getUserDices())
				.setIsWild(session.getIsWilds())
				.setCleanAll(true)
				.build();
		
			NotificationUtils.sendUserDiceNotification(session, message.getUserId(), channel, diceNotification);
		}
	}

}
