package org.skunion.smallru8.BungeeDynamicSync;

import java.util.concurrent.TimeUnit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MessageHandle implements Listener{
	
	@EventHandler
	public void onPubSubMessage(PubSubMessageEvent event) {
		if(event.getChannel().equals(BungeeDynamicSync.PUB_SUB_CHANNEL)) {
			String message = event.getMessage();
			String[] cmd = message.split(" ");
			int len = cmd.length;
			if(cmd[0].equals("CONTROLLER")&&cmd.length==3) {
				if(cmd[1].equals("UPDATE")) {//Other ProxyServer becomes a master controller
					ProxyServer.getInstance().getScheduler().cancel(BungeeDynamicSync.BDS.taskId);//timer reset
					BungeeDynamicSync.BDS.taskId = ProxyServer.getInstance().getScheduler().schedule(BungeeDynamicSync.BDS, BungeeDynamicSync.BDS, 5, 25, TimeUnit.SECONDS).getId();
					BungeeDynamicSync.MASTER = cmd[2];
					BungeeDynamicSync.CONTROLLER.stop();
				}
			}else if(cmd[0].equals("SERVER")&&len>=2) {
				if(cmd[1].equals("ADD")&&len==6) {
					BungeeDynamicSync.addServertoList(cmd[2], cmd[3], cmd[4], cmd[5]);
				}else if(cmd[1].equals("DEL")&&len==3) {
					BungeeDynamicSync.delServerfromList(cmd[2]);
				}else if(cmd[1].equals("STARTED")&&len==3) {//From Spigot plugin tell everyone its game has started 
					BungeeDynamicSync.setGameStartedFlag(cmd[2], true);
				}
			}
			
			
		}
	}
	
	@EventHandler
	public void on(PluginMessageEvent event) {
		if (!event.getTag().equalsIgnoreCase(BungeeDynamicSync.PLUGIN_MSG_CHANNEL))
            return;
		ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();
        String cmd = in.readUTF();
        if(subChannel.equalsIgnoreCase("Connect")&&cmd.equalsIgnoreCase("hub")) {
        	if (event.getReceiver() instanceof ProxiedPlayer)
            {
                ProxiedPlayer receiver = (ProxiedPlayer) event.getReceiver();
                BungeeDynamicSync.sendPlayertoHub(receiver);
            }
        }
	}
	
	public void sendPubSubMessage(String message) {
		BungeeDynamicSync.REDIS_API.sendChannelMessage(BungeeDynamicSync.PUB_SUB_CHANNEL, message);
	}
	
	/**
	 * Delete container message
	 * Send pub/sub message | SERVER     | DEL    | CONTAINER_NAME      |
	 * @param container_id
	 */
	public void sendDELMessage(String container_id) {
		sendPubSubMessage("SERVER DEL "+container_id);
	}
	
	/**
	 * Add container message
	 * Send pub/sub message | SERVER     | ADD    | CONTAINER_NAME      | ip | port | motd |
	 * @param container_id
	 * @param ip
	 * @param port
	 * @param motd
	 */
	public void sendADDMessage(String container_id,String ip,String port,String motd) {
		sendPubSubMessage("SERVER ADD "+container_id+" "+ip+" "+port+" "+motd);
	}
	
}
