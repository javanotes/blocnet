package org.reactiveminds.blocnet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.ds.Node;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockData;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.model.DataStore;
import org.reactiveminds.blocnet.utils.InvalidBlockException;
import org.reactiveminds.blocnet.utils.MiningTimeoutException;
import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.query.Predicate;

@Component
class BlocMinerRunner implements EntryAddedListener<String, AddRequest>, EntryUpdatedListener<String, AddRequest>{

	private static final Logger log = LoggerFactory.getLogger("BlockMinerRunner");
	public static final String MEMPOOL = "MEMPOOL";
	public static final String COMMITNOTIF = "COMMITNOTIF";
	@Value("${chain.mine.awaitChainLockSecs:10}")
	private long awaitChainLock;
	@Autowired
	private HazelcastInstance hazelcast;
	@Autowired
	DataStore blockRepo;
	
	@Autowired
	AsyncTaskExecutor taskExecutor;
	
	@Autowired
	BlocService service;
	private final List<TxnRequest> localMemPool = Collections.synchronizedList(new ArrayList<>());
	
	private IMap<String, AddRequest> globalMemPool() {
		return hazelcast.<String, AddRequest>getMap(MEMPOOL);
	}
	private void setupCommitListener() {
		hazelcast.<BlockData>getTopic(COMMITNOTIF).addMessageListener(new MessageListener<BlockData>() {
					
					@Override
					public void onMessage(Message<BlockData> message) {
						updateLocalMempool(message.getMessageObject());
					}
		});
	}
	private void setupMempool() {
		List<TxnRequest> allRequests = globalMemPool().entrySet(new Predicate<String, AddRequest>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean apply(Entry<String, AddRequest> mapEntry) {
				return true;
			}
		}).parallelStream().map(e -> new TxnRequest(e.getKey(), e.getValue())).collect(Collectors.toList());
		
		localMemPool.addAll(allRequests);
	}
	@PostConstruct
	private void onStart() {
		setupMempool();
		globalMemPool().addEntryListener(this, true);
		setupCommitListener();
	}
	
	private int maxBlockElements;
	
	private boolean isCommitThresholdReached() {
		//TODO isCommitThresholdReached
		return !localMemPool.isEmpty();
		
	}
	
	private class MiningTask implements Runnable{

		protected MiningTask(String t, List<TxnRequest> u) {
			super();
			this.t = t;
			this.u = u;
		}

		final String t;
		final List<TxnRequest> u;

		@Override
		public void run() {
			BlockData chainPool = new BlockData(u);
			chainPool.setChain(t);
			Node minedBloc;
			try 
			{
				String blockData = SerdeUtil.toJson(chainPool);
				//mine next block
				minedBloc = service.mineBlock(t, blockData);
				log.info("["+t+"] Found golden nonce");
				
				//if mined, commit block
				//synchronize for this chain
				ILock lock = hazelcast.getLock(t);
				if(lock.tryLock(awaitChainLock, TimeUnit.SECONDS)) {
					try {
						commitBlock(t, minedBloc, chainPool);
						log.info("["+t+"] New block append succesful ");
						removeMempool(chainPool);
						broadcastCommit(chainPool);
					}
					finally {
						lock.unlock();
					}
				}
				
			} 
			catch (MiningTimeoutException e) {
				log.info(e.getMessage());
				log.debug("Timeout in mining", e);
			}
			catch (InvalidBlockException e) {
				log.info(e.getMessage());
				log.debug("Append block failure", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				log.error("Unexpected error in mining", e);
			}
		}

		private void removeMempool(BlockData chainPool) {
			Set<String> keys = chainPool.getRequests().stream().map(t -> t.getChain()+"."+t.getTxnId()).collect(Collectors.toSet());
			globalMemPool().removeAll(new Predicate<String, AddRequest>() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public boolean apply(Entry<String, AddRequest> mapEntry) {
					return keys.contains(mapEntry.getValue().getChainId()+"."+mapEntry.getKey());
				}
			});
		}
		
	}
	
	@Scheduled(fixedDelayString = "${chains.mine.schedulePeriod:PT30S}")
	public final void miningTask() {
		if(!isCommitThresholdReached())
			return;
		
		//get the current mempool
		BlockData blocData; 
		synchronized (localMemPool) {
			blocData = new BlockData(localMemPool);
		}
		//mine for each chain available
		blocData.groupByChain().forEach(new BiConsumer<String, List<TxnRequest>>() {

			@Override
			public void accept(String t, List<TxnRequest> u) {
				taskExecutor.execute(new MiningTask(t, u));
			}
		});
	}
	private void updateLocalMempool(BlockData mpool) {
		localMemPool.removeAll(mpool.getRequests());
		service.refreshCache(mpool.getChain());
		log.info("New block notification processed for chain - "+mpool.getChain());
	}
	private void broadcastCommit(BlockData mpool) {
		//broadcast is fire and forget
		//the winner node does not really wait for acks
		//this is a private network, hence 'trusted' peers
		//worst case, if for a peer the local mempool is still not updated and is successful in mining a block
		//the append would fail, as the previous block has already been committed
		//we are keeping a race condition so as to who can write next block to persistent storage first
		hazelcast.<BlockData>getTopic(COMMITNOTIF).publish(mpool);
		
	}
	private void commitBlock(String chainId, Node minedBloc, BlockData chainPool) {
		// if we are building on a 'trustworthy' network, like a datacenter, we can probably
		// skip the consensus, and simply there would be a fair contest amongst who can successfully
		// link the next block
		
		// this will be a cluster wide synchronized operation
		// as this is the only write path
		Block bloc = service.appendBlock(chainId, minedBloc);
		
		chainPool.getRequests().stream().map(TxnRequest::getTxnId).forEach(txn -> {
			//save a reference for future lookups
			BlockRef ref = new BlockRef();
			ref.setTxnid(txn);
			ref.setHash(bloc.getCurrHash());
			ref.setChain(chainId);
			
			service.saveBlockRef(ref);
		});
	}

	public int getMaxBlockElements() {
		return maxBlockElements;
	}
	public void setMaxBlockElements(int maxBlockElements) {
		this.maxBlockElements = maxBlockElements;
	}
	@Override
	public void entryUpdated(EntryEvent<String, AddRequest> event) {
		this.entryAdded(event);
	}
	@Override
	public void entryAdded(EntryEvent<String, AddRequest> event) {
		localMemPool.add(new TxnRequest(event.getKey(), event.getValue()));
		log.info("Added txn to mempool: "+SerdeUtil.toJson(event.getValue()));
	}
}
