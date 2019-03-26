package org.reactiveminds.blocnet;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.reactiveminds.blocnet.api.BlocMiner;
import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.core.ScheduledBlocMiner;
import org.reactiveminds.blocnet.core.TriggeredBlocMiner;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.model.dao.BlockRefMapStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;
@Configuration
@EnableScheduling
@EnableAsync
@ConditionalOnProperty(name = "blocnet.miner", havingValue="true")
/**
 * Enabling this configuration makes this instance into a core mining node. No http support will 
 * be provided, and this from architectural perspective, this can be considered as a serverless master node.
 * @author Sutanu_Dalui
 *
 */
class CoreConfig implements SchedulingConfigurer{

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
	
	@Value("${chains.mine.executionMode:t}")
	private String mode;
	@Bean
	BlocMiner miner() {
		return mode.equalsIgnoreCase("s") ? new ScheduledBlocMiner() : new TriggeredBlocMiner();
	}
	
	@Value("${chains.hazelcast.config:}")
	private String configXml;
	@Value("${chains.hazelcast.multicastPort:50071}")
	private int multicastPort;
	@Value("${chains.hazelcast.port:0}")
	private int thisPort;
	@Autowired
	ApplicationContext context;
	private static Config getConfig(Resource configLocation) throws IOException {
		if(configLocation == null)
			return new XmlConfigBuilder().build();
		
		URL configUrl = configLocation.getURL();
		Config config = new XmlConfigBuilder(configUrl).build();
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(configLocation.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		return config;
	}
	
	@Bean
	public HazelcastInstance hazelcastInstance()
			throws IOException {
		Bootstrap.LOG.info("Node starting as MINER ..");
		Resource config = null;
		boolean hasConfigXml = false;
		if(StringUtils.hasText(configXml)) {
			config = context.getResource(configXml);
			hasConfigXml = true;
		}
		final Config conf = getConfig(config);
		conf.setProperty("hazelcast.rest.enabled", "false");
		
		if (!hasConfigXml) {
			NetworkConfig network = conf.getNetworkConfig();
			if (thisPort > 0) {
				network.setPort(thisPort);
			}
			if (multicastPort > 0) {
				JoinConfig join = network.getJoin();
				join.getTcpIpConfig().setEnabled(false);
				join.getAwsConfig().setEnabled(false);
				join.getMulticastConfig().setEnabled(true);
				join.getMulticastConfig().setMulticastPort(multicastPort);
			}
		}
		
		MapStoreConfig storeConf = conf.getMapConfig(BlocService.refTablePattern()).getMapStoreConfig();
		storeConf.setFactoryImplementation(new MapStoreFactory<String, BlockRef>() {

			@Override
			public MapLoader<String, BlockRef> newMapStore(String mapName, Properties properties) {
				BlockRefMapStore store = mapStore();
				store.setMapName(mapName);
				return store;
			}
		});
		storeConf.setWriteDelaySeconds(1);
		storeConf.setEnabled(true);
		
		return Hazelcast.newHazelcastInstance(conf);
	}
	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	BlockRefMapStore mapStore() {
		return new BlockRefMapStore();
	}
}
