package org.reactiveminds.blocnet.core;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.reactiveminds.blocnet.api.BlocMiner;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
/**
 * An implementation of {@linkplain BlocMiner} which triggers mining on certain threshold level
 * reached (mempool size/time elapsed)
 * @author Sutanu_Dalui
 *
 */
public class TriggeredBlocMiner extends AbstractBlocMiner implements BlocMiner,Serializable, EntryAddedListener<String, TxnRequest>, EntryUpdatedListener<String, TxnRequest> {

	@Value("${chains.mine.maxBlockElements:1000}")
	private int maxBlockElements;
	@Scheduled(fixedDelayString = "${chains.mine.schedulePeriod:PT30S}")
	public void miningTaskScheduler() {
		run();
	}
	private Lock taskLock = new ReentrantLock();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@PostConstruct
	private void init() {
		requests = new ArrayBlockingQueue<>(maxBlockElements);
		getMemoryPool().addLocalEntryListener(this, new TxnPredicate.MatchAllTxnPredicate(), false);
	}
	@Override
	public void run() {
		if (taskLock.tryLock()) {
			try {
				doMiningTask();
			} finally {
				taskLock.unlock();
			}
		}
	}

	@Override
	public void setMaxBlockElements(int maxBlockElements) {
		this.maxBlockElements = maxBlockElements;
	}

	@Override
	protected List<TxnRequest> fetchMempool() {
		return getMemoryPool().localKeySet().stream().filter(s -> requests.remove(s)).map(s -> getMemoryPoolEntry(s))
				.collect(Collectors.toList());
	}

	@Override
	protected boolean isCommitReady() {
		return !requests.isEmpty();
	}

	private BlockingQueue<String> requests;
	@Override
	public void entryUpdated(EntryEvent<String, TxnRequest> event) {
		entryAdded(event);
	}
	@Autowired
	AsyncTaskExecutor taskExecutor;
	
	@Override
	public void entryAdded(EntryEvent<String, TxnRequest> event) {
		if(!requests.offer(event.getKey())){
			run();
			entryAdded(event);
		}
	}
}
