package org.skunion.smallru8.BungeeDynamicSync;

import java.io.File;
import java.io.IOException;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {
	
	private Configuration config;
	private File f_config;
	
	public Config() {
		File f_dir = new File("plugins/BungeeDynamicSync");
		f_config = new File(f_dir,"config.yml");
		if(!f_dir.exists()) {
			f_dir.mkdir();
			try {
				f_config.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(f_config);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		config.set(null, f_dir);
	}
	
	public void init() {
		//Spigot servers
		Configuration main = new Configuration();
		Configuration section0_1 = new Configuration();//server name : setting
		main.set("servers", section0_1);
		Configuration section1_1 = new Configuration();
		section1_1.set("min", 1);
		section1_1.set("max", 2);
		section1_1.set("dockerImage","imageName");
		section0_1.set("sample_skywar", section1_1);
		Configuration section1_2 = new Configuration();
		section1_2.set("min", 2);
		section1_2.set("max", 4);
		section1_2.set("dockerImage","imageName");
		section0_1.set("sample_bedwar", section1_2);
		
		Configuration section0_2 = new Configuration();//portainer setting
		main.set("portainers", section0_2);
		Configuration section1_3 = new Configuration();
		section1_3.set("ip", "portainerIP");
		section1_3.set("port", 9000);
		section1_3.set("username", "portainerUsername");
		section1_3.set("passwd", "portainerPasswd");
		section1_3.set("max", 6);
		section0_2.set("portainer1", section1_3);
		Configuration section1_4 = new Configuration();
		section1_4.set("ip", "portainerIP");
		section1_4.set("port", 9000);
		section1_4.set("username", "portainerUsername");
		section1_4.set("passwd", "portainerPasswd");
		section1_4.set("max", 12);
		section0_2.set("portainer1", section1_4);
	}
	
	
	
}
