package org.skunion.smallru8.BungeeDynamicSync;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MessageHandle implements Listener{

	@EventHandler
	public void onPubSubMessage(PubSubMessageEvent event) {
		
	}
	
	public void sendPubSubMessage(String message) {
		BungeeDynamicSync.REDIS_API.sendChannelMessage(BungeeDynamicSync.PUB_SUB_CHANNEL, message);
	}
}
