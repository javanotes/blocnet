package org.reactiveminds.blocnet.core;

import java.util.List;
import java.util.stream.Collectors;

import org.reactiveminds.blocnet.api.BlocMiner;
import org.reactiveminds.blocnet.dto.TxnRequest;
import org.springframework.scheduling.annotation.Scheduled;
/**
 * Implementation of a {@linkplain BlocMiner} using a scheduler.
 * @author Sutanu_Dalui
 *
 */
public class ScheduledBlocMiner extends AbstractBlocMiner implements BlocMiner{
	
	private int maxBlockElements;
	
	@Override
	protected boolean isCommitReady() {
		//TODO isCommitThresholdReached
		return !getMemoryPool().isEmpty();
		
	}
	@Override
	protected List<TxnRequest> fetchMempool() {
		return getMemoryPool().localKeySet().stream().map(s -> getMemoryPoolEntry(s))
				.collect(Collectors.toList());
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.BlocMiner#miningTask()
	 */
	@Override
	@Scheduled(fixedDelayString = "${chains.mine.schedulePeriod:PT30S}")
	public void run() {
		doMiningTask();
	}

	public int getMaxBlockElements() {
		return maxBlockElements;
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.blocnet.core.BlocMiner#setMaxBlockElements(int)
	 */
	@Override
	public void setMaxBlockElements(int maxBlockElements) {
		this.maxBlockElements = maxBlockElements;
	}
}
