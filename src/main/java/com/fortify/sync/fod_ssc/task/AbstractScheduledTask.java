/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.sync.fod_ssc.task;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.fortify.sync.fod_ssc.config.IScheduleConfig;

public abstract class AbstractScheduledTask implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractScheduledTask.class);
	private final String schedule;
	@Autowired private TaskScheduler scheduler;
	
	public AbstractScheduledTask(IScheduleConfig config) {
		this.schedule = config.getCronSchedule();
	}
	
	@PostConstruct
	public void postConstruct() {
		if ("-".equals(StringUtils.defaultIfBlank(schedule,"-")) ) {
			LOG.warn("No schedule defined for {} task; task will not be run automatically", getTaskName());
		} else {
			LOG.info("Schedule for {} task: {}", getTaskName(), schedule);
			scheduler.schedule(this, new CronTrigger(schedule));
		}
	}
	
	public final void run() {
		LOG.info("Running {} task", getTaskName());
		try {
			runTask();
		} finally {
			LOG.debug("Completed {} task", getTaskName());
		}
	}
	
	protected abstract String getTaskName();
	protected abstract void runTask();
	
	
}
