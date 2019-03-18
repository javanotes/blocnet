package org.reactiveminds.blocnet.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
@Configuration
@EnableScheduling
@EnableAsync
@ConditionalOnProperty(name = "blocnet.miner", havingValue="true")
class AsyncConfig implements SchedulingConfigurer{

	@Value("${chains.scheduler.poolSize:4}")
	private int masterPoolSize;
	@Value("${chains.executor.poolSize:10}")
	private int workerPoolSize;
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
	}
	
	@Lazy
	@Bean
	TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(masterPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix("BlocnetScheduler-");
        threadPoolTaskScheduler.initialize();
		return threadPoolTaskScheduler;
	}
	@Bean
	AsyncTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(workerPoolSize);
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("BlocnetExecutor-");
		executor.initialize();
		return executor;
	}
	
}
