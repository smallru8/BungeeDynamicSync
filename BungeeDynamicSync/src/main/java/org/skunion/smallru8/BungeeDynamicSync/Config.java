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
		
	}
	
	
	
}
