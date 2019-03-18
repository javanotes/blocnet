package org.reactiveminds.blocnet.core;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.ds.Blockchain;
import org.reactiveminds.blocnet.ds.Node;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.GetBlockResponse;
import org.reactiveminds.blocnet.dto.Response;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockData;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.model.DataStore;
import org.reactiveminds.blocnet.utils.InvalidBlockException;
import org.reactiveminds.blocnet.utils.InvalidChainException;
import org.reactiveminds.blocnet.utils.MiningTimeoutException;
import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Service
class ServiceImpl implements BlocService {

	private static final Logger log = LoggerFactory.getLogger("BlocService");
	private final ConcurrentMap<String, Blockchain> chainCache = new ConcurrentHashMap<>();
	/**
	 * Loads from data store and verifies the chain as well.
	 * @param name
	 * @param autocreate
	 * @return
	 * @throws InvalidChainException
	 */
	private Blockchain load(String name, boolean autocreate) throws InvalidChainException {
		Deque<Block> loaded = db.loadChain(name);
		Blockchain b;
		
		if((loaded == null || loaded.isEmpty())) {
			if(autocreate) {
				b = Blockchain.newInstance(name, challengeLevel);
				db.save(b);
			}
			else
				b = Blockchain.emptyChain();
		}
		else
			b = Blockchain.buildInstance(name, challengeLevel, loaded);
		
		b.verify();
		return b;
		
	}
	
	private Blockchain getOrLoad(String name) {
		if (!chainCache.containsKey(name)) {
			chainCache.putIfAbsent(name, load(name, true));
		}
		return chainCache.get(name);
	}
	@Autowired
	DataStore db;
	@Autowired
	HazelcastInstance hazelcast;
	
	@Value("${chains.mine.challengeLevel:4}")
	private int challengeLevel;
	
	@Override
	public GetBlockResponse getChain(String name) {
		Blockchain chain = load(name, false);
		if(chain.getSize() == -1)
		{
			GetBlockResponse resp = new GetBlockResponse(Collections.emptyList());
			resp.setStatus(Response.NOT_FOUND);
			return resp;
		}
		List<Block> blocks = StreamSupport.stream(chain.spliterator(), false).collect(Collectors.toList());
		return new GetBlockResponse(blocks);
	}

	@Autowired
	private BeanFactory beans;
	
	@Override
	public String addTransaction(AddRequest request) {
		HazelcastInstance hazel = beans.getBean(HazelcastInstance.class);
		String txnId = Long.toHexString(hazelcast.getFlakeIdGenerator(request.getChainId()).newId());
		TxnRequest txn = new TxnRequest(txnId, request);
		hazel.getMap(BlocMinerRunner.MEMPOOL).set(txn.getPartitionKey(), txn);
		
		return txnId;
		
	}

	@Override
	public Node mineBlock(String chainId, String blockData) {
		Blockchain chain = getOrLoad(chainId);
		//chain will never be null
		Node n;
		try {
			n = chain.mine(blockData);
			return n;
		} 
		catch (TimeoutException e) {
			throw new MiningTimeoutException(e.getMessage());
		}
	}
	@Override
	public Block appendBlock(String chain, Node n) throws InvalidBlockException, InvalidChainException {
		Blockchain b = getOrLoad(chain);
		b.append(n);
		b.verify();
		log.info("Commit append: "+b);
		Block appended = b.getLast();
		db.save(appended);
		return appended;
	}

	@Override
	public void refreshCache(String chain) {
		chainCache.replace(chain, load(chain, false));
	}

	@Override
	public void saveBlockRef(BlockRef ref) {
		db.save(ref);
	}

	@Override
	public TxnRequest fetchTransaction(String chain, String txnid) {
		IMap<String, BlockRef> cache = hazelcast.getMap(BlocService.getRefTableName(chain));
		if(cache.containsKey(txnid)) {
			BlockRef ref = cache.get(txnid);
			List<Block> blocs = db.loadBlock(chain, ref.getHash());
			if(!blocs.isEmpty()) {
				Block b = blocs.stream().findFirst().get();
				BlockData data = SerdeUtil.fromBytes(b.getPayload(), BlockData.class);
				return data.findTxn(txnid);
			}
			
		}
		return new TxnRequest(txnid, new AddRequest());
	}

}
