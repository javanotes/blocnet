package org.reactiveminds.blocnet.api;

import java.util.Map;

import org.reactiveminds.blocnet.dto.TxnRequest;
/**
 * The component interface for executing block mining.
 * @author Sutanu_Dalui
 *
 */
public interface BlocMiner extends Runnable{

	String MEMPOOL = "MEMPOOL";
	String COMMITNOTIF = "COMMITNOTIF";
	/**
	 * 
	 * @param maxBlockElements
	 */
	void setMaxBlockElements(int maxBlockElements);
	/**
	 * Fetch a transaction by key from the pool.
	 * @param key
	 * @return
	 */
	TxnRequest getMemoryPoolEntry(String key);
	/**
	 * Get the pool of pending transactions.
	 * @return
	 */
	Map<String, TxnRequest> getMemoryPool();

}