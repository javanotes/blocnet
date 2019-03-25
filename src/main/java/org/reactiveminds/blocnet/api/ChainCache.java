package org.reactiveminds.blocnet.api;

import org.reactiveminds.blocnet.ds.Blockchain;

public interface ChainCache {

	/**
	 * Get or load
	 * @param name
	 * @return
	 */
	Blockchain getOrLoad(String name);

	/**
	 * (re)load a chain from the data store
	 * @param chain
	 */
	Blockchain refresh(String chain);

	/**
	 * Verify a given chain, by optionally (re)loading it from data store.
	 * @param chain
	 * @param refresh
	 * @return
	 */
	boolean verify(String chain, boolean refresh);

}