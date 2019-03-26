package org.reactiveminds.blocnet;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
/**
 * Enabling this configuration will make this instance as an edge node - exposing REST endpoints to
 * interact with the core mining cluster.
 * @author Sutanu_Dalui
 *
 */
@ConditionalOnProperty(name = "blocnet.miner", havingValue="false", matchIfMissing=true)
@Configuration
class EdgeConfig {
	
	@Value("${chains.hazelcast.config:}")
	String configXml;
	@Autowired
	ApplicationContext context;
	
	@Bean
	public HazelcastInstance hazelcastInstance()
			throws IOException {
		Bootstrap.LOG.info("Node starting as API ..");
		Resource config = null;
		if(StringUtils.hasText(configXml)) {
			config = context.getResource(configXml);
		}
		ClientConfig c = config != null ? new XmlClientConfigBuilder(config.getURL()).build() : new XmlClientConfigBuilder().build();
		//c.getNetworkConfig().setSmartRouting(false);
		c.getNetworkConfig().setConnectionAttemptLimit(15);
		c.getNetworkConfig().setConnectionAttemptPeriod(5000);
		return getHazelcastInstance(c);
	}
	
	private static HazelcastInstance getHazelcastInstance(ClientConfig clientConfig) {
		if (StringUtils.hasText(clientConfig.getInstanceName())) {
			return HazelcastClient
					.getHazelcastClientByName(clientConfig.getInstanceName());
		}
		return HazelcastClient.newHazelcastClient(clientConfig);
	}

}
