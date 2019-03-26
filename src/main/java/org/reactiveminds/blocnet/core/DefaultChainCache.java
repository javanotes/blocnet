package org.reactiveminds.blocnet.core;

import java.util.Deque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.reactiveminds.blocnet.api.ChainCache;
import org.reactiveminds.blocnet.ds.Blockchain;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.DataStore;
import org.reactiveminds.blocnet.utils.err.InvalidChainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

class DefaultChainCache implements ChainCache {

	@Autowired
	DataStore db;
	@Autowired
	CacheManager manager;
	
	private ExecutorService evictThread;
	@PostConstruct
	private void start() {
		evictThread = Executors.newSingleThreadExecutor(r -> new Thread(r, "ChainCacheEvictor"));
		evictThread.execute(()->{
			while(true) {
				try {
					CachedBlockchain b = evictQueue.take();
					expire(b.getItem().getChainName());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		evictThread.shutdown();
	}
	@PreDestroy
	private void stop() throws InterruptedException {
		evictThread.awaitTermination(1, TimeUnit.SECONDS);
	}
	@Value("${chains.mine.challengeLevel:4}")
	private int challengeLevel;
	private DelayQueue<CachedBlockchain> evictQueue = new DelayQueue<>();
	@Value("${chains.cache.expirySecs:60}")
	private long ttl;
	
	/**
	 * Loads from data store and verifies the chain as well.
	 * @param name
	 * @param autocreate
	 * @return
	 * @throws InvalidChainException
	 */
	private Blockchain load(String name, boolean autocreate) throws InvalidChainException {
		Deque<Block> loaded;
		try {
			loaded = db.loadChain(name);
		} catch (IllegalStateException e) {
			throw new InvalidChainException("Duplicate prev_hash detected", e);
		}
		Blockchain b;
		
		if((loaded == null || loaded.isEmpty())) {
			if(autocreate) {
				b = Blockchain.newInstance(name, challengeLevel);
				db.save(b);
			}
			else
				b = Blockchain.emptyChain();
		}
		else
			b = Blockchain.buildInstance(name, challengeLevel, loaded);
		
		b.verify();
		return b;
		
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.ChainCache#getOrLoad(java.lang.String)
	 */
	@Override
	@Cacheable(cacheNames = BaseConfig.BLOCKCHAIN_CACHE)
	public CachedBlockchain getOrLoad(String name) {
		CachedBlockchain c = new CachedBlockchain(load(name, true), TimeUnit.SECONDS.toMillis(ttl));
		evictQueue.offer(c);
		return c;
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.ChainCache#refresh(java.lang.String)
	 */
	@Override
	@CachePut(cacheNames = BaseConfig.BLOCKCHAIN_CACHE)
	public CachedBlockchain refresh(String chain) {
		evictQueue.remove(CachedBlockchain.withName(chain));
		CachedBlockchain c = new CachedBlockchain(load(chain, false), TimeUnit.SECONDS.toMillis(ttl));
		evictQueue.offer(c);
		return c;
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.ChainCache#verify(java.lang.String, boolean)
	 */
	@Override
	public boolean verify(String chain, boolean refresh) {
		Blockchain b = refresh ? refresh(chain).getItem() : getOrLoad(chain).getItem();
		return b.verify();
	}
	@CacheEvict(cacheNames = BaseConfig.BLOCKCHAIN_CACHE)
	public void expire(String name) {}
}
