package org.reactiveminds.blocnet.model.dao;

import java.util.List;

import org.reactiveminds.blocnet.model.Block;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends CrudRepository<Block, Long> {

	/**
	 * @deprecated will be removed
	 * @return
	 */
	@Query(value = "SELECT a.curr_hash " + 
			"FROM blockchains a " + 
			"INNER JOIN " + 
			"blockchains b ON a.prev_hash=b.curr_hash ORDER BY a.id DESC limit 1", nativeQuery = true)
	String findLastHash();
	
	List<Block> findByChain(String chain);
	@Query("select b.id, b.chain, b.timestamp, b.nonce, b.prevHash, b.currHash from Block b where b.chain=?1")
	List<Block> findByChainLite(String chain);
	
	/**
	 * @deprecated mysql 5.6 does not support CTE (connect with prior)
	 * @param chain
	 * @param lastHash
	 * @return
	 */
	@Query(value = "SELECT a.* " + 
			"FROM blockchains a " + 
			"INNER JOIN " + 
			"blockchains b ON a.prev_hash=b.curr_hash and a.chain=b.chain where b.chain=? and b.curr_hash=? ORDER BY a.id DESC", nativeQuery = true)
	List<Block> findByChainHierarchyNext(String chain, String lastHash);
	/**
	 * 
	 * @param chain
	 * @param hash
	 * @return
	 */
	Block findByChainAndCurrHash(String chain, String hash);
	/**
	 * 
	 * @param chain
	 * @param hash
	 * @return
	 */
	List<Block> findByChainAndCurrHashIn(String chain, String ...hash);
}
