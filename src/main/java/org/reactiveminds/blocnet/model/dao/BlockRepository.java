package org.reactiveminds.blocnet.model.dao;

import java.util.List;

import org.reactiveminds.blocnet.model.Block;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends CrudRepository<Block, Long> {

	@Query(value = "SELECT a.curr_hash " + 
			"FROM block a " + 
			"INNER JOIN " + 
			"block b ON a.prev_hash=b.curr_hash ORDER BY a.id DESC limit 1", nativeQuery = true)
	String findLastHash();
	/**
	 * 
	 * @param chain
	 * @return
	 */
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
