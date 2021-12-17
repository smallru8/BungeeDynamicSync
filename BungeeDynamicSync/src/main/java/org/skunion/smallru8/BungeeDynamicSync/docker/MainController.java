package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.ArrayList;

import org.json.JSONObject;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;

import net.md_5.bungee.config.Configuration;

public class MainController {

	private ArrayList<PortainerAuth> portainers;
	private ArrayList<EndPointController> endpoints;
	
	public MainController() {
		//load data from CONFIG
		portainers = new ArrayList<PortainerAuth>();
		endpoints = new ArrayList<EndPointController>();
		Configuration portainerConfLs = BungeeDynamicSync.CONFIG.getPortainerConfig();
		
		//get portainers
		portainerConfLs.getKeys().forEach(key1 -> {
			Configuration portainerconf = portainerConfLs.getSection(key1);
			PortainerAuth portainer = new PortainerAuth(portainerconf.getString("ip"),portainerconf.getInt("port"),portainerconf.getString("username"),portainerconf.getString("passwd"));
			portainers.add(portainer);
			Configuration endpointConfLs = portainerconf.getSection("endpoints");
			
			//get this portainer's endpoints
			endpointConfLs.getKeys().forEach(key2 -> {
				int endpointId = endpointConfLs.getSection(key2).getInt("id");
				int endpointMax = endpointConfLs.getSection(key2).getInt("max");
				JSONObject endpointInfo = new JSONObject(portainer.getEndPointInfo(endpointId));
				String URL_ip = endpointInfo.getString("URL");
				EndPointController endpoint;
				if(URL_ip.startsWith("tcp://")) {//endpoint is remote docker engine
					URL_ip = URL_ip.replace("tcp://", "").replace("/", "");
					URL_ip = URL_ip.split(":")[0];
					endpoint = new EndPointController(portainer,endpointId,endpointMax,URL_ip);
				}else {//endpoint is local docker engine, ip same as portainer 
					endpoint = new EndPointController(portainer,endpointId,endpointMax);
				}
				endpoints.add(endpoint);
			});
		});
	}
	
	/**
	 * TODO Create a container and start
	 * @param dynamic_server
	 * @return String[] : [0] = type(dynamic_server), [1] = ip, [2] = port
	 */
	public String[] createNewRoom(String dynamic_server) {
		
		
		
		return null;
	}
}
