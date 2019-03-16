package org.reactiveminds.blocnet;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
class HazelcastConfig {
	
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
		return Hazelcast.newHazelcastInstance(conf);
	}

}
