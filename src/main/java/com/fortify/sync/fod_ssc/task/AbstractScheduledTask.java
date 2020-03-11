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

import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import com.fortify.sync.fod_ssc.config.IScheduleConfig;

/**
 * Abstract base class for scheduled tasks. Based on the provided {@link IScheduleConfig},
 * this class will determine whether scheduled execution is enabled or not. If enabled,
 * the {@link #runTask()} method provided by the concrete implementation class will be 
 * automatically invoked based on the configured schedule.  
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractScheduledTask<C extends IScheduleConfig> implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractScheduledTask.class);
	private final String DEFAULT_TASK_NAME = this.getClass().getSimpleName();
	@Autowired private TaskScheduler scheduler;
	private CronSequenceGenerator cronSequenceGenerator;
	
	/**
	 * Set up scheduled task execution if a valid schedule has been configured.  
	 */
	@PostConstruct
	public void postConstruct() {
		LOG.info("{} configuration: {}", getTaskName(), getConfig());
		String cronSchedule = getConfig().getCronSchedule();
		if ("-".equals(StringUtils.defaultIfBlank(cronSchedule,"-")) ) {
			LOG.warn("No schedule defined for {}; task will not be run automatically", getTaskName());
		} else {
			scheduler.schedule(this, new CronTrigger(cronSchedule));
			LOG.info("{} scheduled at {}", getTaskName(), getNextExecutionTime());
		}
	}
	
	private final Date getNextExecutionTime() {
		return getCronSequenceGenerator().next(new Date());
	}

	private final CronSequenceGenerator getCronSequenceGenerator() {
		if ( cronSequenceGenerator==null ) {
			cronSequenceGenerator = new CronSequenceGenerator(getConfig().getCronSchedule());
		}
		return cronSequenceGenerator;
	}

	/**
	 * This method is invoked by the scheduler; it logs start and end of scheduled task
	 * execution, invoking the abstract {@link #runTask()} method to have subclasses
	 * perform the actual work.
	 */
	public final void run() {
		LOG.info("Running {}", getTaskName());
		try {
			runTask();
		} finally {
			LOG.info("Completed {}, next scheduled at {}", getTaskName(), getNextExecutionTime());
		}
	}
	
	/**
	 * Return the task name used in logging statements. This default implementation
	 * returns the simple name of the concrete implementation class; subclasses may
	 * override this default implementation.
	 * @return
	 */
	protected String getTaskName() {
		return DEFAULT_TASK_NAME;
	}

	/**
	 * Subclasses need to implement this method to perform the actual work. Implementations
	 * should not log start and end, as this is already handled by {@link #run()}.
	 */
	protected abstract void runTask();
	
	/**
	 * Subclasses need to implement this method to return an {@link IScheduleConfig} instance.
	 * @return
	 */
	protected abstract IScheduleConfig getConfig();
}
