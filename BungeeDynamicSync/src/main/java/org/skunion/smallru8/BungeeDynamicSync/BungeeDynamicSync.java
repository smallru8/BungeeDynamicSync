package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.plugin.Plugin;

import java.util.List;

import org.skunion.smallru8.BungeeDynamicSync.schedules.Clock;

import com.imaginarycode.minecraft.redisbungee.*;

public class BungeeDynamicSync extends Plugin{
	
	public static String MASTER = "";
	
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "BDS_MESSAGE";
	public static String SERVER_ID = "";
	public static MessageHandle mseeageCtrl;
	
	private Clock jobClock;
	
	@Override
	public void onEnable() {
		REDIS_API = RedisBungee.getApi();
		REDIS_API.registerPubSubChannels(PUB_SUB_CHANNEL);
		SERVER_ID = REDIS_API.getServerId();
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
	
}
