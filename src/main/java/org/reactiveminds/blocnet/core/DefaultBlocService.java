package org.reactiveminds.blocnet.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.reactiveminds.blocnet.api.BlocMiner;
import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.api.ChainCache;
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
import org.reactiveminds.blocnet.utils.Crypto;
import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.reactiveminds.blocnet.utils.err.InvalidBlockException;
import org.reactiveminds.blocnet.utils.err.InvalidChainException;
import org.reactiveminds.blocnet.utils.err.MiningTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

class DefaultBlocService implements BlocService {

	private static final Logger log = LoggerFactory.getLogger("BlocService");
	
	@Autowired
	ChainCache chainCache;
	
	@Autowired
	DataStore db;
	@Autowired
	HazelcastInstance hazelcast;
	
	/**
	 * Handler invoked when a commit broadcast is received. By default it would
	 * refresh the {@linkplain ChainCache} for this chain. Subclasses can override this default
	 * behavior.
	 */
	@PostConstruct
	private void setupCommitListener() {
		hazelcast.<BlockData>getTopic(BlocMiner.COMMITNOTIF).addMessageListener(new MessageListener<BlockData>() {
			
			@Override
			public void onMessage(Message<BlockData> message) {
				BlockData block = message.getMessageObject();
				refreshCache(block.getChain());
				log.info("New block notification processed for chain - "+block.getChain());
			}
		});
	}
	
	@Override
	public GetBlockResponse getChain(String name) {
		Blockchain chain = chainCache.getOrLoad(name).getItem();
		if(chain.getSize() == -1)
		{
			GetBlockResponse resp = new GetBlockResponse(Collections.emptyList());
			resp.setStatus(Response.NOT_FOUND);
			return resp;
		}
		if(!chain.verify())
			throw new InvalidChainException(name);
		
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
		hazel.getMap(ScheduledBlocMiner.MEMPOOL).set(txn.getPartitionKey(), txn);
		
		return txnId;
		
	}

	@Override
	public Node mineBlock(String chainId, byte[] blockData) {
		Blockchain chain = chainCache.getOrLoad(chainId).getItem();
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
		Blockchain b = chainCache.getOrLoad(chain).getItem();
		b.append(n);
		b.verify();
		if (log.isDebugEnabled()) {
			log.debug("Commit append: " + b);
		}
		Block appended = b.getLast();
		db.save(appended);
		return appended;
	}

	@Override
	public void refreshCache(String chain) {
		chainCache.refresh(chain);
	}

	@Override
	public void saveBlockRef(BlockRef ref) {
		db.save(ref);
	}

	@Override
	public TxnRequest fetchTransaction(String chain, String txnid) {
		IMap<String, BlockRef> cache = hazelcast.getMap(BlocService.getRefTableName(chain));
		BlockRef ref = cache.get(txnid);
		if (ref != null) {
			boolean valid = ref.isValid();
			if (!valid)
				throw new InvalidBlockException(txnid);
			
			List<Block> blocs = db.findBlock(chain, ref.getHash());
			try 
			{
				if (!blocs.isEmpty()) {
					Block b = blocs.stream().findFirst().get();
					Node n = Blockchain.transform(b);

					valid = Crypto.isValid(n, b.getPrevHash());
					if (!valid)
						throw new InvalidBlockException(txnid);
					
					valid = chainCache.verify(chain, false);
					if (!valid)
						throw new InvalidChainException(chain);

					BlockData data = SerdeUtil.fromBytes(b.getPayload(), BlockData.class);
					return data.findTxn(txnid);
				} 
			} finally {
				if(!valid) {
					ref.setValid(false);
					saveBlockRef(ref);
				}
			} 
		}
		
		return new TxnRequest(txnid, new AddRequest());
	}

	@Override
	public String addTransaction(byte[] request, String chain) {
		HazelcastInstance hazel = beans.getBean(HazelcastInstance.class);
		String txnId = Long.toHexString(hazelcast.getFlakeIdGenerator(chain).newId());
		TxnRequest txn = new TxnRequest(txnId, chain, request);
		hazel.getMap(ScheduledBlocMiner.MEMPOOL).set(txn.getPartitionKey(), txn);
		
		return txnId;
	}
}
