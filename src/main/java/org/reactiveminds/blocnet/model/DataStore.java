package org.reactiveminds.blocnet.model;

import java.util.Deque;
import java.util.List;

import org.reactiveminds.blocnet.ds.Blockchain;

public interface DataStore {
	/**
	 * Save the {@linkplain BlockRef} to a lookup data storage. For JPA by default we are using Hazelcast map
	 * to provide a quick lookup. Can be improved by using, say Cassandra which can allow a collection of simple types as a column value.
	 * @param ref
	 */
	void save(BlockRef ref);
	/**
	 * Save the next block to the parent chain.
	 * @param bloc
	 */
	void save(Block bloc);
	/**
	 * Save a connected list of blocks
	 * @param chain
	 */
	void save(Blockchain chain);
	/**
	 * Fetch a chain by its name
	 * @param name
	 * @return
	 */
	Deque<Block> loadChain(String name);
	/**
	 * Fetch a block for a given chain and optional hash key
	 * @param name
	 * @param hash
	 * @return
	 */
	List<Block> loadBlock(String name, String...hash);
}