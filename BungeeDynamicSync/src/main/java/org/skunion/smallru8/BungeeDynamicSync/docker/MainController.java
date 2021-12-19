package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TimeZone;

import org.json.JSONObject;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;
import org.skunion.smallru8.util.Pair;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

//TODO Detect every type of room's number and auto create

public class MainController implements Job{

	private ArrayList<PortainerAuth> portainers;
	private ArrayList<EndPointController> endpoints;
	private Queue<String> waitForCreate = new LinkedList<String>();
	
	private Trigger queueProcessTri;
	private JobDetail qJob;
	private Scheduler qScheudler;
	
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
		//10 sec
		queueProcessTri = TriggerBuilder.newTrigger().withIdentity("CheckProxyStatus").withSchedule(CronScheduleBuilder.cronSchedule("0/10 * * * * ? *").inTimeZone(TimeZone.getTimeZone("Asia/Taipei"))).build();
		qJob = JobBuilder.newJob(MainController.class).build();
		
		try {
			qScheudler = StdSchedulerFactory.getDefaultScheduler();
			qScheudler.scheduleJob(qJob, queueProcessTri);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Pause this controller
	 */
	public void stop() {
		try {
			if(!qScheudler.isInStandbyMode())
				qScheudler.pauseAll();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start this controller
	 */
	public void start() {
		try {
			if(qScheudler.isStarted())
				qScheudler.resumeAll();
			else
				qScheudler.start();
		} catch (SchedulerException e) {
			e.printStackTrace();
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
	 * @param id
	 */
	public void removeRoom(String id) {
		for(int i=0;i<endpoints.size();i++) {
			if(endpoints.get(i).removeContainer(id, true))
				break;
		}
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Integer[] current;
		int index = 0,min = 2147483647;
		if(waitForCreate.size()!=0) {
			current = new Integer[endpoints.size()];//Each endpoint's current container number
			for(int i=0;i<endpoints.size();i++)
				current[i] = endpoints.get(i).getTotalContainers();
			while(waitForCreate.size()!=0&&BungeeDynamicSync.isMaster()) {
				for(int i=0;i<current.length;i++) {//find minimum
					if(current[i]<min&&current[i]<endpoints.get(i).getMaxContainerLimit()) {
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
			Pair<Integer,Integer> p = currentRoom.get(type);
			
			while(p.first<minCT&&p.second<maxCT) {//free room not enough
				p.first++;
				p.second++;
				createNewRoom(type);
			}
		});
		
	}
	
}
