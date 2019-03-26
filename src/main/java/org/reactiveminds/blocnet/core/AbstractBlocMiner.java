package org.reactiveminds.blocnet.core;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;

import org.reactiveminds.blocnet.api.BlocMiner;
import org.reactiveminds.blocnet.api.BlocService;
import org.reactiveminds.blocnet.ds.Node;
import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.model.BlockData;
import org.reactiveminds.blocnet.model.BlockRef;
import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.reactiveminds.blocnet.utils.err.InvalidBlockException;
import org.reactiveminds.blocnet.utils.err.MiningTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
/**
 * Base implementation that does the common task for mining and committing.
 * @author Sutanu_Dalui
 *
 */
abstract class AbstractBlocMiner implements BlocMiner {

	final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());
	
	@Value("${chain.mine.awaitChainLockSecs:10}")
	private long awaitChainLock;
	@Autowired
	BlocService service;
	@Autowired
	HazelcastInstance hazelcast;
	@Autowired
	AsyncTaskExecutor taskExecutor;
	
	private void removeMempool(BlockData chainPool) {
		//remove the committed transactions from the global mempool
		Set<TxnRequest> keys = new HashSet<>(chainPool.getRequests());
		globalMemPool().removeAll(new TxnPredicate.RemoveTxnPredicate(keys));
	}
	IMap<String, TxnRequest> globalMemPool() {
		return hazelcast.<String, TxnRequest>getMap(MEMPOOL);
	}
	@PostConstruct
	private void setupCommitListener() {
		log.info("Mine worker setup done. Ready to run ..");
	}
	TxnRequest mempoolEntry(String key) {
		return Optional.ofNullable(globalMemPool().get(key)).orElse(new TxnRequest(key, new AddRequest()));
	}
	/**
	 * Get a mempool snapshot for this mining task. Subclasses will
	 * implement the data fetch.
	 * @return
	 */
	protected abstract List<TxnRequest> fetchMempool();
	/**
	 * Check if the mining threshold has been reached, so a commit operation can be tried now. Subclasses will
	 * implement this logic.
	 * @return
	 */
	protected abstract boolean isCommitReady();
	/**
	 * The core mining task that is being performed.
	 */
	protected void doMiningTask() {
		if(!isCommitReady())
			return;
		
		log.info("Mining task execution launched ..");
		//get the current mempool
		//take the local entries as the snapshot
		BlockData blocData = new BlockData(fetchMempool());
		
		//mine for each chain available
		blocData.groupByChain().forEach(new BiConsumer<String, List<TxnRequest>>() {

			@Override
			public void accept(String t, List<TxnRequest> u) {
				taskExecutor.execute(new MiningTask(t, u));
			}
		});
	}
	private void broadcastCommit(BlockData mpool) {
		// broadcast is fire and forget
		// the winner node does not really wait for acks
		// this is a private network, hence 'trusted' peers
		// worst case, if for a peer the local mempool is still not updated and is successful in mining a block
		// the append would fail, as the previous block has already been committed
		// we are keeping a race condition so as to who can write next block to persistent storage first
		hazelcast.<BlockData>getTopic(COMMITNOTIF).publish(mpool);
		
	}
	private void commitBlock(String chainId, Node minedBloc, BlockData chainPool) {
		// if we are building on a 'trustworthy' network, like a datacenter, we can probably
		// skip the consensus, and simply there would be a fair contest amongst who can successfully
		// link the next block
		
		// this will be a cluster wide synchronized operation
		// as this is the only write path. we are relying on the
		// underlying database to give us a consistent state of
		// data (read committed). So that would help us to resolve
		// a race condition, and only one of the mining node
		// will be able to append the next block.
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
	
	private class MiningTask implements Runnable, Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected MiningTask(String t, List<TxnRequest> u) {
			super();
			this.chain = t;
			this.requests = u;
			
			if (log.isDebugEnabled()) {
				log.debug("Requests for chain: " + chain + "\n\t" + requests);
			}
		}

		final String chain;
		final List<TxnRequest> requests;
		
		@Override
		public void run() {
			BlockData chainPool = new BlockData(requests);
			chainPool.setChain(chain);
			Node minedBloc;
			try 
			{
				String blockData = SerdeUtil.toJson(chainPool);
				// mine next block
				service.refreshCache(chain);
				minedBloc = service.mineBlock(chain, blockData);
				log.info("["+chain+"] Found golden nonce");
				
				// if mined, commit block
				
				// not providing a cluster wide synchronization (?)
				// since each node will be mining on its own share (Hz local entries)
				// for the DB commit, we are using strongly consistent RDBMS and thus
				// looking on read committed isolation before saving it
				
				// when using am eventually consistent store like Cassandra, the DB write
				// at least might need to be synchronized cluster wide
				boolean commit = false;
				ILock lock = hazelcast.getLock(chain);
				if(lock.tryLock(awaitChainLock, TimeUnit.SECONDS)) {
					try {
						commitBlock(chain, minedBloc, chainPool);
						commit = true;
						log.info("["+chain+"] New block append succesful ");
					}
					finally {
						lock.unlock();
					}
				}
				if(commit) {
					removeMempool(chainPool);
					broadcastCommit(chainPool);
				}
			} 
			catch (MiningTimeoutException e) {
				log.info(e.getMessage());
				log.debug("Timeout in mining", e);
			}
			catch (InvalidBlockException e) {
				log.info(e.getMessage());
				log.debug("Append block failure", e);
			} 
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				log.error("Unexpected error in mining", e);
			}
		}
		
	}
}
