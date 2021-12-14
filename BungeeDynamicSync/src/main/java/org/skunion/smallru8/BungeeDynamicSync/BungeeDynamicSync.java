package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.plugin.Plugin;
import com.imaginarycode.minecraft.redisbungee.*;

public class BungeeDynamicSync extends Plugin{
	
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "NODE_MESSAGE";
	public static String SERVER_ID = "";
	public static MessageHandle mseeageCtrl;
	
	@Override
	public void onEnable() {
		REDIS_API = RedisBungee.getApi();
		REDIS_API.registerPubSubChannels(PUB_SUB_CHANNEL);
		SERVER_ID = REDIS_API.getServerId();
		mseeageCtrl = new MessageHandle();
		
	}
	
	@Override
	public void onDisable() {
		
	}
	
}
