package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skunion.smallru8.BungeeDynamicSync.docker.MainController;
import org.skunion.smallru8.BungeeDynamicSync.schedules.Clock;

import com.imaginarycode.minecraft.redisbungee.*;

public class BungeeDynamicSync extends Plugin{
	
	public static BungeeDynamicSync BDS;
	
	public static String MASTER = "";
	
	public static Config CONFIG;
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "BDS_MESSAGE";
	public static String SERVER_ID = "";
	
	public static MainController CONTROLLER;
	
	public static MessageHandle mseeageCtrl;
	
	//Room list
	public static Map<ServerInfo,Boolean> ROOM_IS_STARTED = new HashMap<ServerInfo,Boolean>();//If a room is started
	
	private Clock jobClock;
	
	@Override
	public void onEnable() {
		BDS = this;
		REDIS_API = RedisBungee.getApi();
		REDIS_API.registerPubSubChannels(PUB_SUB_CHANNEL);
		SERVER_ID = REDIS_API.getServerId();
		
		CONFIG = new Config();
		
		mseeageCtrl = new MessageHandle();
		setMasterController();
		
		CONTROLLER = new MainController();
		
		jobClock = new Clock();
		jobClock.start();//Auto update current controller
		
	}
	
	@Override
	public void onDisable() {
		REDIS_API.unregisterPubSubChannels(PUB_SUB_CHANNEL);
	}
	
	public static void setMasterController() {
		List<String> proxies =  BungeeDynamicSync.REDIS_API.getAllServers();
		String proxyId = proxies.get(0);
		for(String tmp : proxies) {
			proxyId = tmp.compareTo(proxyId) < 0 ? tmp : proxyId;
		}
		
		if(!proxyId.equals(BungeeDynamicSync.MASTER)) {//Msater controller changed
			BungeeDynamicSync.MASTER = proxyId;
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
	
	public static boolean isGameStarted(String dynamic_server_name) {
		return ROOM_IS_STARTED.get(ProxyServer.getInstance().getServerInfo(dynamic_server_name));
	}
	
	public static boolean isMaster() {
		return MASTER.equals(SERVER_ID);
	}
	
}
