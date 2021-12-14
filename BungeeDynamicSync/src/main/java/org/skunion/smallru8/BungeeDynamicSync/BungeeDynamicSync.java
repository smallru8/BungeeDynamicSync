package org.skunion.smallru8.BungeeDynamicSync;

import net.md_5.bungee.api.plugin.Plugin;
import com.imaginarycode.minecraft.redisbungee.*;

public class BungeeDynamicSync extends Plugin{
	
	public static RedisBungeeAPI REDIS_API;
	public static final String PUB_SUB_CHANNEL = "NODE_MESSAGE";
	
	@Override
	public void onEnable() {
		REDIS_API = RedisBungee.getApi();
		REDIS_API.registerPubSubChannels(PUB_SUB_CHANNEL);
		
	}
	
	@Override
	public void onDisable() {
		
	}
	
}
