package org.skunion.smallru8.BungeeDynamicSync.docker;

import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class PortainerAuth {
	
	private String ip;
	private int port;
	private String account;
	private String passwd;
	private String token;
	
	private int expireTimeSec;
	
	public PortainerAuth(String ip,int port,String account,String passwd) {
		this.ip = ip;
		this.port = port;
		this.account = account;
		this.passwd = passwd;
		expireTimeSec = 0;
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getURL(int endPointId) {
		return "http://"+ip+":"+port+"/api/endpoints/"+endPointId+"/docker/";
	}
	
	public String getEndPointInfo(int endPointId) {
		HttpClient httpclient = HttpClientBuilder.create().build();
		try {
			String url = "http://"+ip+":"+port+"/api/endpoints/"+endPointId;
			HttpGet req = new HttpGet(url);
			req.setHeader("Content-Type", "application/json");
		    req.setHeader("Authorization", "Bearer "+getToken());
		    HttpResponse response = httpclient.execute(req);
		    HttpEntity entity = response.getEntity();
		    return EntityUtils.toString(entity);
		}catch (Exception e) {
		    System.out.println(e.getMessage());
	    }
		return "{}";
	}
	
	public boolean refreshToken() {
		HttpClient httpclient = HttpClientBuilder.create().build();
		String postRequest = "{\"Username\" : \""+account+"\", \"Password\" : \""+passwd+"\"}"; 
		        		
		try {
		    String url = "http://"+ip+":"+port+"/api/auth";
		    HttpPost request = new HttpPost(url);
		    request.setHeader("Content-Type", "application/json");
	
		    // Request body
		    StringEntity reqEntity = new StringEntity(postRequest,"UTF-8");
		    request.setEntity(reqEntity);
		    HttpResponse response = httpclient.execute(request);
		    HttpEntity entity = response.getEntity();
	
		    String json_raw = EntityUtils.toString(entity);
		    JSONObject jwt = new JSONObject(json_raw);
		    token = jwt.getString("jwt");
		    jwt.clear();
		    
		    JSONObject payload = new JSONObject(new String(Base64.getDecoder().decode(token.split(".")[1]),"UTF-8"));
		    expireTimeSec = payload.getInt("exp");
		    payload.clear();
		    
	    } catch (Exception e) {
		    System.out.println(e.getMessage());
		    return false;
	    }
		return true;
	}
	
	public String getToken() {
		if(expireTimeSec-System.currentTimeMillis()/1000 < 180)//Token expire time less than 3 minutes
			refreshToken();
		return token;
	}
	
}
