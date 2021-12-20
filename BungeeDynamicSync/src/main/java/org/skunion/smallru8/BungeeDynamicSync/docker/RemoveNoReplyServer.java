package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;

public class RemoveNoReplyServer implements Runnable{

	private static Map<String,Integer> NO_REPLY_SERVER = new HashMap<String,Integer>();
	
	@Override
	public void run() {
		Collection<String> dynServerTypes = BungeeDynamicSync.CONFIG.getServerConfig().getKeys();
		Map<String,ServerInfo> servers = ProxyServer.getInstance().getServers();
		
		//Ping all dynamic server, if no reply for 4 times, it would be killed
		for (Map.Entry<String, ServerInfo> entry : servers.entrySet()) {
            if(!dynServerTypes.contains(entry.getValue().getMotd()))//Not a dynamic server
            	continue;
            
            entry.getValue().ping(new Callback<ServerPing>() {
				@Override
				public void done(ServerPing result, Throwable error) {
					if(error!=null){
						if(!NO_REPLY_SERVER.containsKey(entry.getKey())) {//No reply once
							NO_REPLY_SERVER.put(entry.getKey(), 0);
						}
					}else {//has reply
						NO_REPLY_SERVER.remove(entry.getKey());
					}
				}
            });
        }

		//No reply for 4 times will be remove
		for (Iterator<Entry<String, Integer>> it =  NO_REPLY_SERVER.entrySet().iterator();it.hasNext();) {
			Entry<String, Integer> e = it.next();
			e.setValue(e.getValue()+1);
			if(e.getValue() > 3) {
				BungeeDynamicSync.CONTROLLER.removeRoom(e.getKey());
				it.remove();
			}
		}
	}

}
