package org.skunion.smallru8.BungeeDynamicSync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {
	
	public Configuration config;
	private File f_config;
	private File f_dir_docker;
	
	public Config() {
		File f_dir = new File("plugins/BungeeDynamicSync");
		f_dir_docker = new File(f_dir,"docker");
		File f_docker = new File(f_dir_docker,"CreateContainer.json");
		if(!f_dir_docker.exists())
			f_dir_docker.mkdir();
		if(!f_docker.exists()) {
			try {
				f_docker.createNewFile();
				InputStream is = getClass().getClassLoader().getResourceAsStream("CreateContainer.json");
				InputStreamReader sr = new InputStreamReader(is, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(sr);
				FileWriter fw = new FileWriter(f_docker);
				String line = "";
				while((line = br.readLine())!=null) {
					fw.write(line+"\n");
				}
				fw.flush();
				fw.close();
				br.close();
				sr.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		f_config = new File(f_dir,"config.yml");
		if(!f_dir.exists()) {
			f_dir.mkdir();
			try {
				f_config.createNewFile();
				init();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			try {
				config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(f_config);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void save() {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, f_config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Configuration getServerConfig() {
		return config.getSection("servers");
	}
	
	public Configuration getPortainerConfig() {
		return config.getSection("portainers");
	}
	
	public File getContainerSettingDir() {
		return f_dir_docker;
	}
	
	private void init() {
		//Spigot servers
		Configuration mainConfig = new Configuration();
		Configuration section0_1 = new Configuration();//server name : setting
		
		Configuration section1_1 = new Configuration();
		section1_1.set("min", 1);
		section1_1.set("max", 2);
		section1_1.set("ContainerCreateScript","CreateContainer.json");
		section0_1.set("sample_skywar", section1_1);
		Configuration section1_2 = new Configuration();
		section1_2.set("min", 2);
		section1_2.set("max", 4);
		section1_2.set("ContainerCreateScript","CreateContainer.json");
		section0_1.set("sample_bedwar", section1_2);
		mainConfig.set("servers", section0_1);
		
		Configuration section0_2 = new Configuration();//portainer setting
		
		Configuration section1_3 = new Configuration();
		section1_3.set("ip", "portainerIP");
		section1_3.set("port", 9000);
		section1_3.set("username", "portainerUsername");
		section1_3.set("passwd", "portainerPasswd");
		
		Configuration section2_1 = new Configuration();
		Configuration section3_1 = new Configuration();
		section3_1.set("id", 1);
		section3_1.set("max", 6);
		section2_1.set("p1", section3_1);
		section1_3.set("endpoints", section2_1);
		
		section0_2.set("portainer1", section1_3);
		Configuration section1_4 = new Configuration();
		section1_4.set("ip", "portainerIP");
		section1_4.set("port", 9000);
		section1_4.set("username", "portainerUsername");
		section1_4.set("passwd", "portainerPasswd");
		
		Configuration section2_2 = new Configuration();
		Configuration section3_2 = new Configuration();
		section3_2.set("id", 1);
		section3_2.set("max", 12);
		section2_2.set("p1", section3_2);
		section1_4.set("endpoints", section2_2);
		
		section0_2.set("portainer2", section1_4);
		
		mainConfig.set("portainers", section0_2);
		config = mainConfig;
		save();
	}
	
}
