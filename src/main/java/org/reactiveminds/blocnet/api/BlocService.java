package org.reactiveminds.blocnet.api;

import java.util.concurrent.TimeoutException;

import org.reactiveminds.blocnet.ds.Node;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.GetBlockResponse;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.utils.err.InvalidBlockException;
import org.reactiveminds.blocnet.utils.err.InvalidChainException;
import org.reactiveminds.blocnet.utils.err.MiningTimeoutException;
/**
 * Core service facade for blocnet operations
 * @author Sutanu_Dalui
 *
 */
public interface BlocService {

	String REF_CACHE_PATTERN = "__ref_";
	String TXN_CACHE_PATTERN = "__txn_";
	
	public static String refTablePattern() {
		return REF_CACHE_PATTERN+"*";
	}
	public static String getRefTableName(String chain) {
		return REF_CACHE_PATTERN+chain;
	}
	public static String getTxnCacheName(String chain) {
		return TXN_CACHE_PATTERN+chain;
	}
	public static String chainFromRefTableName(String name) {
		if(name.startsWith(REF_CACHE_PATTERN)) {
			return name.substring(REF_CACHE_PATTERN.length());
		}
		return name;
	}
	/**
	 * 
	 * @param name
	 * @return
	 */
	GetBlockResponse getChain(String name);
	/**
	 * 
	 * @param chain
	 * @param blockData
	 * @return
	 * @throws TimeoutException 
	 */
	Node mineBlock(String chain, byte[] blockData) throws MiningTimeoutException;
	/**
	 * 
	 * @param chain
	 * @param n
	 * @return 
	 * @throws InvalidBlockException
	 */
	Block appendBlock(String chain, Node n) throws InvalidBlockException, InvalidChainException;
	/**
	 * 
	 * @param request
	 * @return
	 */
	String addTransaction(AddRequest request);
	String addTransaction(byte[] request, String chain);
	/**
	 * 
	 * @param id
	 */
	void refreshCache(String id);
	/**
	 * 
	 * @param ref
	 */
	void saveBlockRef(BlockRef ref);
	/**
	 * 
	 * @param chain
	 * @param txnid
	 * @return
	 */
	TxnRequest fetchTransaction(String chain, String txnid);
}
