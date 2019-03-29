package org.reactiveminds.blocnet.core;

import java.time.Duration;

import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.api.ChainCache;
import org.reactiveminds.blocnet.utils.TimeCheckBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
/**
 * Common bean configurations in both edge and core mode.
 * @author Sutanu_Dalui
 *
 */
@Configuration
@EnableCaching
public class BaseConfig implements ApplicationContextAware{

	public static final String BLOCKCHAIN_CACHE = "BLOCKCHAIN_CACHE";
	@Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(BLOCKCHAIN_CACHE);
    }
	@Bean
	BlocService service() {
		return new DefaultBlocService();
	}
	@Bean
	ChainCache cache() {
		return new LocalChainCache();
	}
	@Bean
	@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	TimeCheckBean timecheckBean(int maxIteration, Duration maxTimeLapse) {
		return new TimeCheckBean(maxIteration, maxTimeLapse);
	}
	
	public static TimeCheckBean newTimeCheckBean() {
		Environment env = context.getBean(Environment.class);
		int iter = Integer.parseInt(env.getProperty("chains.mine.timer.maxIter", "1000000"));
		Duration dur = Duration.parse(env.getProperty("chains.mine.timer.maxTime", "PT10M"));
		return context.getBean(TimeCheckBean.class, iter, dur);
	}
	
	private static ApplicationContext context;
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
}
