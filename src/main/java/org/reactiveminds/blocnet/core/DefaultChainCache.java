package org.reactiveminds.blocnet.core;

import java.util.Deque;

import org.reactiveminds.blocnet.api.ChainCache;
import org.reactiveminds.blocnet.ds.Blockchain;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.DataStore;
import org.reactiveminds.blocnet.utils.err.InvalidChainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

class DefaultChainCache implements ChainCache {

	@Autowired
	DataStore db;

	@Value("${chains.mine.challengeLevel:4}")
	private int challengeLevel;
	
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
	public Blockchain getOrLoad(String name) {
		return load(name, true);
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.ChainCache#refresh(java.lang.String)
	 */
	@Override
	@CachePut(cacheNames = BaseConfig.BLOCKCHAIN_CACHE)
	public Blockchain refresh(String chain) {
		return load(chain, false);
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.ChainCache#verify(java.lang.String, boolean)
	 */
	@Override
	public boolean verify(String chain, boolean refresh) {
		Blockchain b = refresh ? refresh(chain) : getOrLoad(chain);
		return b.verify();
	}
}
