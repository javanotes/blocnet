package org.reactiveminds.blocnet.api;

public interface BlocMiner extends Runnable{

	public static final String MEMPOOL = "MEMPOOL";
	public static final String COMMITNOTIF = "COMMITNOTIF";
	void setMaxBlockElements(int maxBlockElements);

}