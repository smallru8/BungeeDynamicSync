package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PortainerController {
	
	private PortainerAuth portainerAuth;
	private boolean LOCK = false;
	private HttpClient httpclient;
	
	public PortainerController(PortainerAuth portainerAuth) {
		this.portainerAuth = portainerAuth;
		httpclient = HttpClientBuilder.create().build();
	}
	
	/**
	 * Get all containers which have label "manager=BDS" and (BDStype=<type>)
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
	
	public JSONArray getContainerJsonArray() {
		return getContainerJsonArray("");
	}
	
	public int getTotalContainers() {
		return getContainerJsonArray().length();
	}
	
	public int getTotalContainers(String type) {
		return getContainerJsonArray(type).length();
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
	
	public String createContainer(String dynamic_server) {
		if(!LOCK) {
			String image = ServerImageMap.DYNAMIC_SERVER_MAP_DOCKER_IMAGE.get(dynamic_server);
			
		}
		
		return "";
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
}
