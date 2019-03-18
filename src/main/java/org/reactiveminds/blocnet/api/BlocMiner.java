package org.reactiveminds.blocnet.api;

public interface BlocMiner {

	public static final String MEMPOOL = "MEMPOOL";
	public static final String COMMITNOTIF = "COMMITNOTIF";
	void miningTask();
	void setMaxBlockElements(int maxBlockElements);

}