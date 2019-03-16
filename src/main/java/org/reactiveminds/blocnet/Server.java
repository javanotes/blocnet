package org.reactiveminds.blocnet;

import java.time.Duration;

import org.reactiveminds.blocnet.ds.TimeCheckBean;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

@SpringBootApplication(exclude = {HazelcastAutoConfiguration.class, HazelcastJpaDependencyAutoConfiguration.class})
public class Server implements ApplicationContextAware{
	public static void main(String[] args) {
		SpringApplication.run(Server.class, args);
	}
	private static ApplicationContext context;
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

	public static TimeCheckBean newTimeCheckBean() {
		Environment env = context.getBean(Environment.class);
		int iter = Integer.parseInt(env.getProperty("chains.mine.timer.maxIter", "1000000"));
		Duration dur = Duration.parse(env.getProperty("chains.mine.timer.maxTime", "PT10M"));
		return context.getBean(TimeCheckBean.class, iter, dur);
	}
}
