package org.reactiveminds.blocnet.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.model.BlockRef;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.MapStore;

public class BlockRefMapStore implements MapStore<String, BlockRef> {

	@Autowired
	BlockRefRepository repo;
	
	public BlockRefMapStore() {
	}

	@Override
	public BlockRef load(String key) {
		String chain = BlocService.chainFromRefTableName(mapName);
		return repo.findByChainAndTxnid(chain, key);
	}

	@Override
	public Map<String, BlockRef> loadAll(Collection<String> keys) {
		String chain = BlocService.chainFromRefTableName(mapName);
		return repo.findByChainAndTxnidIn(chain, new ArrayList<>(keys)).stream().collect(Collectors.toMap(BlockRef::getTxnid, Function.identity()));
	}

	@Override
	public Iterable<String> loadAllKeys() {
		return null;
	}

	@Override
	public void store(String key, BlockRef value) {
		repo.save(value);
	}

	@Override
	public void storeAll(Map<String, BlockRef> map) {
		repo.saveAll(map.values());
	}

	@Override
	public void delete(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteAll(Collection<String> keys) {
		throw new UnsupportedOperationException();
	}
	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}
	private String mapName;

}
