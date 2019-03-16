package org.reactiveminds.blocnet.ds;

import java.time.Duration;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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