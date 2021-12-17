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

public class PortainerController {
	
	private PortainerAuth portainerAuth;
	private boolean LOCK = false;
	private HttpClient httpclient;
	private Random random = new Random();
	
	public PortainerController(PortainerAuth portainerAuth) {
		this.portainerAuth = portainerAuth;
		httpclient = HttpClientBuilder.create().build();
	}
	
	/**
	 * Get all containers which have label "manager=BDS" and (BDStype=type)
	 * 
	 * @param type if type==null, it will show all containers.
	 * @return containers' setting
	 */
	public JSONArray getContainerJsonArray(String type) {
		
		String url = portainerAuth.getURL()+"containers/json?filters=";
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
		String url = portainerAuth.getURL()+"containers/json?filters=";
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
		return portainerAuth.getMaxContainer();
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
	 * @return Container's id or null if create failed
	 */
	public String createContainer(String dynamic_server) {
		if(!LOCK) {
			String name = dynamic_server+"_"+SHA.SHA1(dynamic_server+random.nextInt());
			String json_raw = getCreateJson_RAW(BungeeDynamicSync.CONFIG.getServerConfig(dynamic_server).getString("ContainerCreateScript")).replace("$TYPE", dynamic_server);
			String url = portainerAuth.getURL()+"containers/create?name="+name;
			
			try {
				HttpPost req = new HttpPost(url);
				req.setHeader("Content-Type", "application/json");
			    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
			    StringEntity reqEntity = new StringEntity(json_raw,"UTF-8");
			    req.setEntity(reqEntity);
			    HttpResponse response = httpclient.execute(req);
			    HttpEntity entity = response.getEntity();
			    
			    JSONObject result = new JSONObject(EntityUtils.toString(entity));
			    return result.getString("Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param id
	 * @return String[] : [0] = type(dynamic_server), [1] = port
	 */
	public String[] startContainer(String id) {
		String[] rets = {"","",""};
		String url = portainerAuth.getURL()+"containers/"+id+"/start";
		try {
			HttpPost req = new HttpPost(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    httpclient.execute(req);
		    
		    JSONObject detail = getContainerDetailById(id);
		    if(detail!=null) {
		    	rets[0] = detail.getJSONObject("Labels").getString("BDStype");
		    	//TODO NEXT
		    }
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return rets;
	}
	
	/**
	 * Remove container from portainer.
	 * @param id container id
	 * @param force
	 */
	public void removeContainer(String id,boolean force) {
		String url = portainerAuth.getURL()+"containers/"+id;
		url = force ? (url+="?force=true") : (url+="?force=false");
		try {
			HttpDelete req = new HttpDelete(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+portainerAuth.getToken());
		    httpclient.execute(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
