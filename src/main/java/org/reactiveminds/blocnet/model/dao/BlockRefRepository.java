package org.reactiveminds.blocnet.model.dao;

import java.util.List;

import org.reactiveminds.blocnet.model.BlockRef;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface BlockRefRepository extends CrudRepository<BlockRef, Long> {
	
	/**
	 * 
	 * @param c chain name
	 * @param t txn id
	 * @return
	 */
	BlockRef findByChainAndTxnid(String c, String t);
	List<BlockRef> findByChainAndTxnidIn(String c, List<String> t);
	List<BlockRef> findByChain(String c);
}
