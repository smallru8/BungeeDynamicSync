package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;
import org.skunion.smallru8.util.SHA;

public class EndPointController {

	private PortainerAuth portainerAuth;
	private boolean LOCK = false;
	private HttpClient httpclient;
	private Random random = new Random();
	private int endPointId;
	private int maxContainer;
	private String endPointHostIP;
	
	public EndPointController(PortainerAuth portainerAuth,int endpointId,int max,String ip) {
		this.portainerAuth = portainerAuth;
		httpclient = HttpClientBuilder.create().build();
		endPointId = endpointId;
		maxContainer = max;
		endPointHostIP = ip;
	}
	
	public EndPointController(PortainerAuth portainerAuth,int endpointId,int max) {
		this.portainerAuth = portainerAuth;
		httpclient = HttpClientBuilder.create().build();
		endPointId = endpointId;
		maxContainer = max;
		endPointHostIP = portainerAuth.getIP();
	}
	
	
	/**
	 * Get all containers which have label "manager=BDS" and (BDStype=type)
	 * 
	 * @param type if type==null, it will show all containers.
	 * @return containers' setting
	 */
	public JSONArray getContainerJsonArray(String type) {
		
		String url = portainerAuth.getURL(endPointId)+"containers/json?filters=";
		try {
			String filter = "{\"label\":[\"manager=BDS\"";
			filter = type=="" ? (filter+="]}") : (filter+=",\"BDStype="+type+"\"]}");
			url += URLEncoder.encode(filter, "UTF-8");
			
			HttpGet req = new HttpGet(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    HttpResponse response = httpclient.execute(req);
		    HttpEntity entity = response.getEntity();
		    
		    return new JSONArray(EntityUtils.toString(entity));
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JSONArray("[]");
	}
	
	public JSONObject getContainerDetailById(String id) {
		String url = portainerAuth.getURL(endPointId)+"containers/json?filters=";
		try {
			String filter = "{\"id\":[\""+id+"\"]}";
			
			url += URLEncoder.encode(filter, "UTF-8");
			
			HttpGet req = new HttpGet(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    HttpResponse response = httpclient.execute(req);
		    HttpEntity entity = response.getEntity();
		    
		    JSONArray jArr= new JSONArray(EntityUtils.toString(entity));
		    if(jArr.length()>0)
		    	return jArr.getJSONObject(0);
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONArray getContainerJsonArray() {
		return getContainerJsonArray("");
	}
	
	public int getTotalContainers() {
		return getContainerJsonArray().length();
	}
	
	public int getTotalContainers(String dynamic_server) {
		return getContainerJsonArray(dynamic_server).length();
	}
	
	public int getMaxContainerLimit() {
		return maxContainer;
	}
	
	public void clearContainer() {
		LOCK = true;
		JSONArray containerLs = getContainerJsonArray();
		containerLs.forEach(ct_json -> {
			String id = ((JSONObject)ct_json).getString("Id");
			removeContainer(id,true);
		});
		
		LOCK = false;
	}
	
	/**
	 * Create a container
	 * @param dynamic_server
	 * @return Container's name or null if create failed
	 */
	public String createContainer(String dynamic_server) {
		if(!LOCK) {
			String name = dynamic_server+"_"+SHA.SHA1(dynamic_server+random.nextInt());
			String json_raw = getCreateJson_RAW(BungeeDynamicSync.CONFIG.getServerConfig().getSection(dynamic_server).getString("ContainerCreateScript")).replace("$TYPE", dynamic_server).replace("$CT_NAME",name);
			String url = portainerAuth.getURL(endPointId)+"containers/create?name="+name;
			
			try {
				HttpPost req = new HttpPost(url);
				req.setHeader("Content-Type", "application/json");
			    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
			    StringEntity reqEntity = new StringEntity(json_raw,"UTF-8");
			    req.setEntity(reqEntity);
			    HttpResponse response = httpclient.execute(req);
			    HttpEntity entity = response.getEntity();
			    
			    JSONObject result = new JSONObject(EntityUtils.toString(entity));
			    BungeeDynamicSync.BDS.getLogger().info("Create container: "+dynamic_server+", id: "+result.getString("Id")+" at endpoint: "+endPointHostIP);
			    if(result.getString("Id")==null)
			    	return null;
			    return name;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		BungeeDynamicSync.BDS.getLogger().info("Create container: "+dynamic_server+" failed. Endpoint: "+endPointHostIP);
		return null;
	}
	
	/**
	 * Start container by id or name
	 * @param id
	 * @return String[] : [0] = type(dynamic_server), [1] = ip, [2] = port
	 */
	public String[] startContainer(String id) {
		String[] rets = {"","",""};
		String url = portainerAuth.getURL(endPointId)+"containers/"+id+"/start";
		try {
			HttpPost req = new HttpPost(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    httpclient.execute(req);
		    
		    JSONObject detail = getContainerDetailById(id);
		    if(detail!=null) {
		    	rets[0] = detail.getJSONObject("Labels").getString("BDStype");
		    	rets[1] = endPointHostIP;
		    	
		    	JSONArray ports = detail.getJSONArray("Ports");
		    	for(int i=0;i<ports.length();i++) {
		    		if(ports.getJSONObject(i).getInt("PrivatePort")==25565&&ports.getJSONObject(i).getString("Type").equals("tcp")) {
		    			rets[2] = ""+ports.getJSONObject(i).getInt("PublicPort");
		    			break;
		    		}
		    	}
		    }
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		//Bungeecord use these info to update server list
		return rets;
	}
	
	/**
	 * Remove container from portainer.
	 * @param id container id or name
	 * @param force
	 */
	public boolean removeContainer(String id,boolean force) {
		String url = portainerAuth.getURL(endPointId)+"containers/"+id;
		url = force ? (url+="?force=true") : (url+="?force=false");
		try {
			HttpDelete req = new HttpDelete(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    HttpResponse response = httpclient.execute(req);
		    if(response.getStatusLine().getStatusCode()==204)
		    	return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private String getCreateJson_RAW(String jsonName) {
		String ret = "";
		try {
			FileReader fr = new FileReader(new File(BungeeDynamicSync.CONFIG.getContainerSettingDir(),jsonName));
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null)
				ret += line+"\n";
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
