package org.reactiveminds.blocnet.cfg;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.reactiveminds.blocnet.Bootstrap;
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
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
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
@ConditionalOnProperty(name = "blocnet.miner", havingValue="true")
class MinerConfig {

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
		conf.setProperty("hazelcast.rest.enabled", "true");
		
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
