package org.reactiveminds.blocnet.model.dao;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.ds.Blockchain;
import org.reactiveminds.blocnet.ds.HashUtil;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.model.DataStore;
import org.reactiveminds.blocnet.utils.InvalidChainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
/**
 * This may be implemented other way
 * @author Sutanu_Dalui
 *
 */
@Service
class JpaDataStore implements DataStore {

	private static final Logger log = LoggerFactory.getLogger("JpaDataStore");
	@Autowired
	BlockRepository repo;
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.model.DBService#save(org.reactiveminds.hazelblock.ds.Blockchain)
	 */
	@Override
	public void save(Blockchain chain) {
		repo.saveAll(chain);
	}
	
	@Override
	public Deque<Block> loadChain(String name) {
		return loadFromBeginning(name);
	}
	private LinkedList<Block> loadFromBeginning(String name) {
		return loadSlice(name, HashUtil.GENESIS_PREV_HASH, null);
	}
	private LinkedList<Block> loadFromOffset(String name, String hash) {
		return loadSlice(name, hash, null);
	}
	/**
	 * Slice chain with the given hash offsets
	 * @param name chain name
	 * @param fromHash load chain from the next link after this (exclusive)
	 * @param tillHash load chain till this link (inclusive)
	 * @return
	 */
	private LinkedList<Block> loadSlice(String name, String fromHash, String tillHash) {
		List<Block> blocks = repo.findByChain(name);
		if(blocks == null || blocks.isEmpty())
			return new LinkedList<Block>();
		
		Map<String, Block> mapped = blocks.stream().collect(Collectors.toMap(Block::getPrevHash, Function.identity()));
		
		Block bloc = mapped.get(HashUtil.GENESIS_PREV_HASH);
		if(bloc == null)
			throw new InvalidChainException("No genesis block found in loaded chain");
		
		final LinkedList<Block> linked = new LinkedList<>();
		boolean offsetReached = false;
		
		if (fromHash != null && fromHash.equals(HashUtil.GENESIS_PREV_HASH)) {
			linked.add(bloc);
			offsetReached = true;
		}
		
		while(mapped.containsKey(bloc.getCurrHash())) {
			//get the next node by its previous hash
			bloc = mapped.get(bloc.getCurrHash());
			if(fromHash != null && !offsetReached) {
				offsetReached = fromHash.equals(bloc.getCurrHash());
				continue;
			}
			linked.add(bloc);
			if(tillHash != null && tillHash.equals(bloc.getCurrHash()))
				break;
		}
		log.debug("Linked blocks: "+linked);
		return linked;
	}
	@Override
	public void save(Block bloc) {
		repo.save(bloc);
		log.info("Saved bloc: "+bloc);
	}
	@Override
	public List<Block> findBlock(String name, String... hash) {
		List<Block> blocks = repo.findByChainAndCurrHashIn(name, hash);
		if(blocks == null || blocks.isEmpty())
			return Collections.emptyList();
		
		return blocks;
		
	}
	@Autowired
	HazelcastInstance hazelcast;
	
	@Override
	public void save(BlockRef ref) {
		//saving in memory in Hazelcast. so if the cluster is down this data will be lost
		//TODO implement backing store for BlockRef
		IMap<String, BlockRef> cache = hazelcast.getMap(BlocService.getRefTableName(ref.getChain()));
		cache.set(ref.getTxnid(), ref);
	}

	@Override
	public Deque<Block> loadChain(Block block) {
		return loadFromOffset(block.getChain(), block.getCurrHash());
	}
}
