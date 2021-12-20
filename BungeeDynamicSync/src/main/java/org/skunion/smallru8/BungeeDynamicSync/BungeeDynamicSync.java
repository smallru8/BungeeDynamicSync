package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.skunion.smallru8.BungeeDynamicSync.docker.MainController;

import com.imaginarycode.minecraft.redisbungee.*;

//TODO auto move config from resources

public class BungeeDynamicSync extends Plugin implements Runnable{
	
	public static BungeeDynamicSync BDS;
	
	public static String MASTER = "";
	
	public static Config CONFIG;
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "BDS_MESSAGE";
	public static final String PLUGIN_MSG_CHANNEL = "BDS:channel";
	public static String SERVER_ID = "";
	
	public static MainController CONTROLLER;
	
	public static MessageHandle mseeageCtrl;
	
	//Room list
	public static Map<ServerInfo,Boolean> ROOM_IS_STARTED = new HashMap<ServerInfo,Boolean>();//If a room is started
	
	public Integer taskId = null;
	
	@Override
	public void onEnable() {
		BDS = this;
		getProxy().registerChannel(PLUGIN_MSG_CHANNEL);
		REDIS_API = RedisBungee.getApi();
		REDIS_API.registerPubSubChannels(PUB_SUB_CHANNEL);
		SERVER_ID = REDIS_API.getServerId();
		
		CONFIG = new Config();
		
		mseeageCtrl = new MessageHandle();
		CONTROLLER = new MainController();
		
		//Auto update current controller
		taskId = ProxyServer.getInstance().getScheduler().schedule(this, this, 5, 25, TimeUnit.SECONDS).getId();
	}
	
	@Override
	public void onDisable() {
		ProxyServer.getInstance().getScheduler().cancel(this);
		REDIS_API.unregisterPubSubChannels(PUB_SUB_CHANNEL);
	}
	
	@Override
	public void run() {
		setMasterController();
	}
	
	public static void setMasterController() {
		List<String> proxies =  BungeeDynamicSync.REDIS_API.getAllServers();
		String proxyId = proxies.get(0);
		for(String tmp : proxies) {
			proxyId = tmp.compareTo(proxyId) < 0 ? tmp : proxyId;
		}
		
		if(!proxyId.equals(BungeeDynamicSync.MASTER)) {//Msater controller changed
			BungeeDynamicSync.MASTER = proxyId;
			ProxyServer.getInstance().getLogger().info("Change master controller to "+proxyId+".");
			if(proxyId.equals(BungeeDynamicSync.SERVER_ID)) {//This Bungeecord is master controller now
				BungeeDynamicSync.mseeageCtrl.sendPubSubMessage("CONTROLLER UPDATE "+proxyId);
				CONTROLLER.start();
			}else {
				CONTROLLER.stop();
			}
		}
	}
	
	public static void addServertoList(String dynamic_server_name,String ip,String port,String motd) {
		InetSocketAddress address = new InetSocketAddress(ip,Integer.parseInt(port));
		ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(dynamic_server_name, address, motd, false);
		ProxyServer.getInstance().getServers().put(dynamic_server_name,serverInfo);
		ROOM_IS_STARTED.put(serverInfo, false);
	}
	
	public static void delServerfromList(String dynamic_server_name) {
		ROOM_IS_STARTED.remove(ProxyServer.getInstance().getServerInfo(dynamic_server_name));
		ProxyServer.getInstance().getServers().remove(dynamic_server_name);//Before remove this, players would be teleported to hub by dynamic_server
	}
	
	/**
	 * This room's game has started.
	 * @param dynamic_server_name
	 * @param b
	 */
	public static void setGameStartedFlag(String dynamic_server_name,boolean b) {
		ROOM_IS_STARTED.replace(ProxyServer.getInstance().getServerInfo(dynamic_server_name), b);
	}
	
	public static boolean isMaster() {
		return MASTER.equals(SERVER_ID);
	}
	
	public static void sendPlayertoHub(ProxiedPlayer player) {
		Iterator<String> hub_it = ProxyServer.getInstance().getConfigurationAdapter().getListeners().iterator().next().getServerPriority().iterator();
		sendPlayertoHub(player,hub_it);
	}
	
	public static void sendPlayertoHub(ProxiedPlayer player,Iterator<String> hub_it) {
		if(hub_it.hasNext()&&player.isConnected()) {
			player.connect(ProxyServer.getInstance().getServerInfo(hub_it.next()),new Callback<Boolean>() {
				@Override
				public void done(Boolean result, Throwable error) {
					if(!result)
						sendPlayertoHub(player,hub_it);
				}
			});
		}else {
			player.disconnect(new TextComponent("Unable to connect hub."));
		}
	}
	
}
