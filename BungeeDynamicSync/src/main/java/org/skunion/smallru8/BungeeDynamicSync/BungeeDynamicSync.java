package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;

import org.skunion.smallru8.BungeeDynamicSync.schedules.Clock;

import com.imaginarycode.minecraft.redisbungee.*;

public class BungeeDynamicSync extends Plugin{
	
	public static BungeeDynamicSync BDS;
	
	public static String MASTER = "";
	
	public static Config CONFIG;
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "BDS_MESSAGE";
	public static String SERVER_ID = "";
	public static MessageHandle mseeageCtrl;
	
	
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
		jobClock = new Clock();
		jobClock.start();//Auto update current controller
		//getProxy().getServers(); TODO
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
			if(proxyId.equals(BungeeDynamicSync.SERVER_ID))//This Bungeecord is master controller now
				BungeeDynamicSync.mseeageCtrl.sendPubSubMessage("CONTROLLER UPDATE "+proxyId);
		}
	}
	
	public static void addServertoList(String dynamic_server_name,String ip,String port,String motd) {
		InetSocketAddress address = new InetSocketAddress(ip,Integer.parseInt(port));
		ProxyServer.getInstance().getServers().put(dynamic_server_name,  ProxyServer.getInstance().constructServerInfo(dynamic_server_name, address, motd, false));
	}
	
	public static void delServerfromList(String dynamic_server_name) {
		//Players would be teleported to hub by dynamic_server
		ProxyServer.getInstance().getServers().remove(dynamic_server_name);
	}
	
	public static boolean isMaster() {
		return MASTER.equals(SERVER_ID);
	}
	
}
