package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;
import org.skunion.smallru8.util.Pair;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

public class MainController implements Runnable{

	private ArrayList<PortainerAuth> portainers;
	private ArrayList<EndPointController> endpoints;
	private Queue<String> waitForCreate = new LinkedList<String>();
	private Queue<String> waitForRemove = new LinkedList<String>();
	
	private Integer taskId1 = null,taskId2 = null;
	private Runnable pingJob = new RemoveNoReplyServer();
	
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
	 * Pause this controller
	 */
	public void stop() {
		if(taskId1!=null&&taskId2!=null) {
			ProxyServer.getInstance().getScheduler().cancel(taskId1);
			ProxyServer.getInstance().getScheduler().cancel(taskId2);
			taskId1 = null;
			taskId2 = null;
		}
	}
	
	/**
	 * Start this controller
	 */
	public void start() {
		if(taskId1==null&&taskId2==null) {
			syncPortainer();
			taskId1 = ProxyServer.getInstance().getScheduler().schedule(BungeeDynamicSync.BDS, this, 5, 10, TimeUnit.SECONDS).getId();
			taskId2 = ProxyServer.getInstance().getScheduler().schedule(BungeeDynamicSync.BDS, pingJob, 5, 25, TimeUnit.SECONDS).getId();
		}
	}
	
	/**
	 * Add a container as a game room
	 * @param dynamic_server game type
	 */
	public void createNewRoom(String dynamic_server) {
		waitForCreate.add(dynamic_server);
	}
	
	/**
	 * Remove container by id or name
	 * Use to remove freeze container
	 * @param id
	 */
	public void removeRoom(String id) {
		waitForRemove.add(id);
	}

	@Override
	public void run() {
		////////////////////////////////////////////////////////////////////////////////////////////
		//Process waitForRemove queue
		if(waitForRemove.size()!=0) {
			while(waitForRemove.size()!=0&&BungeeDynamicSync.isMaster()) {
				String name = waitForRemove.poll();
				for(int i=0;i<endpoints.size();i++) {
					if(endpoints.get(i).removeContainer(name, true)) {
						BungeeDynamicSync.mseeageCtrl.sendDELMessage(name);//Tell others remove 
						BungeeDynamicSync.delServerfromList(name);
						
						break;
					}
				}
				
			}
			
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////
		//Process waitForCreate queue
		Integer[] current;
		int index = 0;
		if(waitForCreate.size()!=0) {
			current = new Integer[endpoints.size()];//Each endpoint's current container number
			for(int i=0;i<endpoints.size();i++)
				current[i] = endpoints.get(i).getTotalContainers();
			while(waitForCreate.size()!=0&&BungeeDynamicSync.isMaster()) {
				int min = 2147483647;
				for(int i=0;i<current.length;i++) {//find minimum
					if(current[i]<min&&(current[i]<endpoints.get(i).getMaxContainerLimit()||endpoints.get(i).getMaxContainerLimit()<=0)) {
						min = current[i];
						index = i;
					}
				}
				if(min==2147483647)//No any free endpoints
					break;
				
				String container_name = endpoints.get(index).createContainer(waitForCreate.poll());
				if(container_name==null)//Create failed
					continue;
				String[] containerData = endpoints.get(index).startContainer(container_name);
				if(containerData==null)
					continue;
				//Setting, broadcast, setMotd as type
				BungeeDynamicSync.addServertoList(container_name, containerData[1], containerData[2], containerData[0]);
				BungeeDynamicSync.mseeageCtrl.sendADDMessage(container_name, containerData[1], containerData[2], containerData[0]);//broadcast
				
				current[index]++;
			}
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////
		//Detect every type of room's number and auto create
		//Check free room, if not enough, create a new one
		
		Collection<String> dynServerTypes = BungeeDynamicSync.CONFIG.getServerConfig().getKeys();
		Map<String,Pair<Integer,Integer>> currentRoom = new HashMap<String,Pair<Integer,Integer>>();
		dynServerTypes.forEach( type->{
			Pair<Integer,Integer> p = new Pair<Integer,Integer>();//current free room, current total room
			p.makePair(0, 0);
			currentRoom.put(type, p);
		});
		
		for(Entry<ServerInfo, Boolean> e : BungeeDynamicSync.ROOM_IS_STARTED.entrySet()) {
			currentRoom.get(e.getKey().getMotd()).second++;
			if(!e.getValue())//not started
				currentRoom.get(e.getKey().getMotd()).first++;
		}
		
		dynServerTypes.forEach(type->{
			int minCT = BungeeDynamicSync.CONFIG.getServerConfig().getSection(type).getInt("min");
			int maxCT = BungeeDynamicSync.CONFIG.getServerConfig().getSection(type).getInt("max");
			if(maxCT<=0)
				maxCT = 2147483647;
			Pair<Integer,Integer> p = currentRoom.get(type);
			
			while(p.first<minCT&&p.second<maxCT) {//free room not enough
				p.first++;
				p.second++;
				createNewRoom(type);
			}
		});
	}
	
	public void syncPortainer() {
		endpoints.forEach(ep->{
			JSONArray ja = ep.getContainerJsonArray();
			for(int i=0;i<ja.length();i++) {
				JSONObject jo = ja.getJSONObject(i);
				String id = jo.getString("Id");
				String ip = ep.getEndPointHostIP();
				String port = "";
				String type_motd = jo.getJSONObject("Labels").getString("BDStype");
				JSONArray ports = jo.getJSONArray("Ports");
		    	for(int j=0;j<ports.length();j++) {
		    		if(ports.getJSONObject(j).getInt("PrivatePort")==25565&&ports.getJSONObject(j).getString("Type").equals("tcp")) {
		    			port = ""+ports.getJSONObject(j).getInt("PublicPort");
		    			break;
		    		}
		    	}
		    	BungeeDynamicSync.addServertoList(id, ip, port, type_motd);
			}
		});
	}
	
}
