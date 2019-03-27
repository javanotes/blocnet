package org.reactiveminds.blocnet.utils;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ExpiryNotifier extends Observable implements Runnable{
	/**
	 * Interface to be extended by elements that wish to be notified on expiration.
	 * @author Sutanu_Dalui
	 *
	 */
	@FunctionalInterface
	public static interface Perishable{
		/**
		 * Callback method that will be invoked when this {@linkplain Perishable} item is expired
		 * , either by idle time elapsed, or life time elapsed
		 */
		void fireOnExpiry();
		
	}
	private final PerishableItem poisonPill = new PerishableItem(() -> {}, 0, 0, TimeUnit.MILLISECONDS);
	
	private final DelayQueue<PerishableItem> evictQueue = new DelayQueue<>();
	/**
	 * Stop this {@linkplain ExpiryNotifier} run.
	 */
	public void stop() {
		evictQueue.offer(poisonPill);
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
	private <T extends Perishable> void register(final T item, long timeToLive, long timeToIdle, TimeUnit unit) {
		PerishableItem eItem = new PerishableItem(item, timeToLive, timeToIdle, unit);
		evictQueue.offer(eItem);
		addObserver(eItem);
	}
	/**
	 * Register a new item to be notified on lifetime expiration.
	 * @param item
	 * @param timeToLive
	 * @param unit
	 */
	public <T extends Perishable> void register(T item, long timeToLive, TimeUnit unit) {
		register(item, timeToLive, 0, unit);
	}
	@Override
	public void run() {
		while(true) {
			try 
			{
				PerishableItem item = evictQueue.take();
				if(item == poisonPill) {
					break;
				}
				else {
					setChanged();
					notifyObservers(item);
					deleteObserver(item);
				}
			} 
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
	}
	/**
	 * Wrapper class for a {@linkplain Perishable} item.
	 * @author Sutanu_Dalui
	 *
	 */
	private static class PerishableItem implements Delayed, Observer{
		private final Perishable item;
		/**
		 * 
		 * @param timeToLive
		 * @param timeToIdle
		 * @param unit
		 */
		public PerishableItem(Perishable item, long timeToLive, long timeToIdle, TimeUnit unit) {
			this.item = item;
			ttl = unit.toMillis(timeToLive);
			tti = unit.toMillis(timeToIdle);
			created = System.currentTimeMillis();
			modified = created;
		}
		
		private final long created;
		private volatile long modified;
		private final long ttl;
		private final long tti;
		@Override
		public int compareTo(Delayed o) {
			 return Long.compare(this.getDelayMillis(), ((PerishableItem) o).getDelayMillis());
		}
		private long getDelayMillis() {
			long now = System.currentTimeMillis();
			long mod = (modified + tti) - now;
			return (tti > 0 && mod <= 0) ? mod : ((created + ttl) - now);
		}
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
		}
		@Override
		public void update(Observable o, Object arg) {
			if(arg == this)
				item.fireOnExpiry();
		}
		
	}

	/** Test expiration **/
	/*private static class SomePerishableCommodity implements Perishable{
		
		protected SomePerishableCommodity(String item) {
			super();
			this.item = item;
		}
		private String item;
		@Override
		public void fireOnExpiry() {
			System.out.println("["+toInstantString() + "]: {"+item+"} I perish!");
		}
		
	}
	private static String toInstantString() {
		//return Instant.now().toString();
		return DateFormat.getDateTimeInstance().format(new Date());
	}
	public static void main(String[] args) {
		ExpiryNotifier notif = new ExpiryNotifier();
		new Thread(notif).start();
		notif.register(new SomePerishableCommodity("[1]"), 5, TimeUnit.SECONDS);
		notif.register(new SomePerishableCommodity("[2]"), 1, TimeUnit.SECONDS);
		//notif.stop();
	}*/
}
