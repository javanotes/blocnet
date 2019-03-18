package org.reactiveminds.blocnet.model.dao;

import java.util.List;

import org.reactiveminds.blocnet.model.Block;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends CrudRepository<Block, Long> {

	@Query(value = "SELECT a.curr_hash " + 
			"FROM blockchains a " + 
			"INNER JOIN " + 
			"blockchains b ON a.prev_hash=b.curr_hash ORDER BY a.id DESC limit 1", nativeQuery = true)
	String findLastHash();
	
	@Query(value = "SELECT a.* " + 
			"FROM blockchains a " + 
			"INNER JOIN " + 
			"blockchains b ON a.prev_hash=b.curr_hash and a.chain=b.chain where a.chain=? ORDER BY a.id DESC", nativeQuery = true)
	List<Block> findByChainHierarchy(String chain);
	
	List<Block> findByChain(String chain);
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
