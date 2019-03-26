package org.reactiveminds.blocnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(exclude = {HazelcastAutoConfiguration.class, HazelcastJpaDependencyAutoConfiguration.class})
public class Bootstrap {
	public static void main(String[] args) {
		SpringApplication.run(Bootstrap.class, args);
	}
	public static final Logger LOG = LoggerFactory.getLogger("Bootstrap");
	
	
}
