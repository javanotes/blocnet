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
import org.reactiveminds.blocnet.utils.InvalidBlockException;
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
		List<Block> blocks = repo.findByChain(name);
		if(blocks == null || blocks.isEmpty())
			return new LinkedList<>();
		
		Map<String, Block> mapped = blocks.stream().collect(Collectors.toMap(Block::getPrevHash, Function.identity()));
		Block genesis = mapped.get(HashUtil.GENESIS_PREV_HASH);
		if(genesis == null)
			throw new InvalidBlockException("No genesis block found in loaded chain!");
		
		LinkedList<Block> linked = new LinkedList<>();
		linked.add(genesis);
		
		while(mapped.containsKey(genesis.getCurrHash())) {
			genesis = mapped.get(genesis.getCurrHash());
			linked.add(genesis);
		}
		log.info("Linked blocks: "+linked);
		return linked;
	}
	@Override
	public void save(Block bloc) {
		log.info("Saving bloc: "+bloc);
		repo.save(bloc);
	}
	@Override
	public List<Block> loadBlock(String name, String... hash) {
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
}
