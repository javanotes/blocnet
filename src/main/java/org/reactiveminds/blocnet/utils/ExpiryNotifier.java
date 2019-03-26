package org.reactiveminds.blocnet.utils;

import java.util.Observable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

public class ExpiryNotifier extends Observable implements Runnable{
	
	@FunctionalInterface
	public static interface ItemExpiredListener<T>{
		void onExpiry(T item);
	}
	private DelayQueue<Expireable<?>> evictQueue = new DelayQueue<>();
	public void stop() {
		
	}
	private static class Poison extends Expireable<Object>{
		public Poison() {
			super(0, TimeUnit.MILLISECONDS);
		}
	}
	/**
	 * Register a new item to be notified on expiration.
	 * @param <T>
	 * @param item
	 * @param timeToLive
	 * @param timeToIdle
	 * @param unit
	 * @param listener
	 */
	public <T> void register(T item, long timeToLive, long timeToIdle, TimeUnit unit, ItemExpiredListener<T> listener) {
		Expireable<T> eItem = new Expireable<>(timeToLive, timeToIdle, unit);
		eItem.setItem(item);
		eItem.setListener(listener);
		addObserver(eItem);
	}

	@Override
	public void run() {
		while(true) {
			try {
				Expireable<?> item = evictQueue.take();
				if(item instanceof Poison) {
					break;
				}
				else {
					setChanged();
					notifyObservers();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
	}
}
