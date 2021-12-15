package org.skunion.smallru8.BungeeDynamicSync.schedules;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.skunion.smallru8.BungeeDynamicSync.BungeeDynamicSync;

public class CheckProxyStatus implements Job{

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		BungeeDynamicSync.setMasterController();		
	}

}
