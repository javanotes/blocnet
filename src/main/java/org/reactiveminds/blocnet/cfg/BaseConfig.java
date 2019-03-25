package org.reactiveminds.blocnet.cfg;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class BaseConfig {

	public static final String BLOCKCHAIN_CACHE = "BLOCKCHAIN_CACHE";
	@Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(BLOCKCHAIN_CACHE);
    }

}
