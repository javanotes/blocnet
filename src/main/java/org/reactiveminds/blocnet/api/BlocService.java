package org.reactiveminds.blocnet.api;

import java.util.concurrent.TimeoutException;

import org.reactiveminds.blocnet.ds.Node;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.GetBlockResponse;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.utils.InvalidBlockException;
import org.reactiveminds.blocnet.utils.InvalidChainException;
import org.reactiveminds.blocnet.utils.MiningTimeoutException;

public interface BlocService {

	public static String getRefTableName(String chain) {
		return chain + "__ref";
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
	Node mineBlock(String chain, String blockData) throws MiningTimeoutException;
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
