package org.reactiveminds.blocnet.utils;

import java.time.Duration;

public class TimeCheckBean {
	public TimeCheckBean(int maxIteration, Duration maxTimeLapse) {
		super();
		this.maxIteration = maxIteration;
		this.maxTimeLapse = maxTimeLapse;
		epoch = System.currentTimeMillis();
	}
	private int iteration = 0;
	public boolean isTimeout() {
		return System.currentTimeMillis()-epoch >= maxTimeLapse.toMillis() || iteration++ == maxIteration;
	}
	private final long epoch;
	final int maxIteration;
	final Duration maxTimeLapse;
	
	@Override
	public String toString() {
		return "TimeCheck [maxIteration=" + maxIteration + ", maxTimeLapse=" + maxTimeLapse + "]";
	}
	
}