package org.skunion.smallru8.BungeeDynamicSync;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MessageHandle implements Listener{

	/*
	 * Change master controller                                      | CONTROLLER | UPDATE   | <proxy id>       |
	 * Add a server from portainer and list                          | PORTAINER  | ADD      | <dynamic server> | <ip> | <port> |TODO 30秒測一次,由Master controller添加後發出，讓其他proxies同步list
	 * Delete a server from portainer and list                       | PORTAINER  | DEL      | <dynamic server> | TODO 改成30秒測一次是否已被刪除,不用讓dynamic server自己回報
	 * TODO 透過plugin message channel 發送dynamic server list同步給hub server
	 * TODO dynamic server開始後啟動白名單
	 * */
	
	
	@EventHandler
	public void onPubSubMessage(PubSubMessageEvent event) {
		if(event.getChannel().equals(BungeeDynamicSync.PUB_SUB_CHANNEL)) {
			String message = event.getMessage();
			String[] cmd = message.split(" ");
			
			if(cmd[0].equals("CONTROLLER")&&cmd.length==3) {
				if(cmd[1].equals("UPDATE"))
					BungeeDynamicSync.MASTER = cmd[2];
				
			}
			
			
		}
	}
	
	public void sendPubSubMessage(String message) {
		BungeeDynamicSync.REDIS_API.sendChannelMessage(BungeeDynamicSync.PUB_SUB_CHANNEL, message);
	}
	
	/**
	 * Delete container message
	 * Send pub/sub message | SERVER     | DEL    | <CONTAINER_NAME>      |
	 * @param container_id
	 */
	public void sendDELMessage(String container_id) {
		sendPubSubMessage("SERVER DEL "+container_id);
	}
	
}
