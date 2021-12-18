package org.skunion.smallru8.BungeeDynamicSync.schedules;

import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class Clock {

	private JobDetail job,job2;
	private Trigger tri;
	private Scheduler scheudler_check_master;
	
	public Clock() {
		job = JobBuilder.newJob(CheckProxyStatus.class).build();
		job2 = JobBuilder.newJob(CheckDynamicServerStatus.class).build();
		//every 30 seconds
		tri = TriggerBuilder.newTrigger().withIdentity("CheckProxyStatus").withSchedule(CronScheduleBuilder.cronSchedule("0/30 * * * * ? *").inTimeZone(TimeZone.getTimeZone("Asia/Taipei"))).build();
		try {
			scheudler_check_master = StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		try {
			scheudler_check_master.scheduleJob(job, tri);
			scheudler_check_master.scheduleJob(job2, tri);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		try {
			scheudler_check_master.clear();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
}
