package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.ArrayList;
import java.util.LinkedList;
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

import net.md_5.bungee.config.Configuration;

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
			qScheudler.start();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * TODO Create a container and start
	 * @param dynamic_server
	 * @return String[] : [0] = type(dynamic_server), [1] = ip, [2] = port
	 */
	public String[] createNewRoom(String dynamic_server) {
		
		
		
		return null;
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
				current[index]++;
				
				String dynamic_server = waitForCreate.poll();
				String container_name = endpoints.get(index).createContainer(dynamic_server);
				if(container_name==null)//Create failed
					continue;
				String[] containerData = endpoints.get(index).startContainer(container_name);
				//TODO setting、broadcast、setMotd as type
			}
		}
	}
	
}
