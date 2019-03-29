package org.reactiveminds.blocnet.api;

import org.reactiveminds.blocnet.core.CachedBlockchain;
import org.reactiveminds.blocnet.ds.Blockchain;

/**
 * Local in memory repository for active blockchains
 * @author Sutanu_Dalui
 *
 */
public interface ChainCache {
	/**
	 * A light version of get(), that will not pull the payload,
	 * however, will refer to database.
	 * @param name
	 * @return
	 */
	Blockchain fetch(String name);
	/**
	 * Get or load
	 * @param name
	 * @return
	 */
	CachedBlockchain getOrLoad(String name);

	/**
	 * (re)load a chain from the data store
	 * @param chain
	 */
	CachedBlockchain refresh(String chain);

	/**
	 * Verify a given chain, by optionally (re)loading it from data store.
	 * @param chain
	 * @param refresh
	 * @return
	 */
	boolean verify(String chain, boolean refresh);

}