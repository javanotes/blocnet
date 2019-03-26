package org.reactiveminds.blocnet.utils;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.reactiveminds.blocnet.utils.ExpiryNotifier.ItemExpiredListener;

class Expireable<T> implements Delayed, Observer{
	private T item;
	private ItemExpiredListener<T> listener;
	/**
	 * 
	 * @param timeToLive
	 * @param timeToIdle
	 * @param unit
	 */
	public Expireable(long timeToLive, long timeToIdle, TimeUnit unit) {
		ttl = unit.toMillis(timeToLive);
		tti = unit.toMillis(timeToIdle);
		created = System.currentTimeMillis();
		modified = created;
	}
	/**
	 * 
	 * @param timeToIdle
	 * @param unit
	 */
	public Expireable(long timeToIdle, TimeUnit unit) {
		this(Long.MAX_VALUE, timeToIdle, unit);
	}
	/**
	 * Update the modified time
	 */
	public void touch() {
		modified = System.currentTimeMillis();
	}
	private final long created;
	private volatile long modified;
	private final long ttl;
	private final long tti;
	@SuppressWarnings("rawtypes")
	@Override
	public int compareTo(Delayed o) {
		 return Long.compare(this.getDelayMillis(), ((Expireable) o).getDelayMillis());
	}
	private long getDelayMillis() {
		long now = System.currentTimeMillis();
		long mod = (modified + tti) - now;
		return mod <= 0 ? mod : ((created + ttl) - now);
	}
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
	}
	public T getItem() {
		return item;
	}
	public void setItem(T item) {
		this.item = item;
	}
	@Override
	public void update(Observable o, Object arg) {
		if (listener != null) {
			listener.onExpiry(item);
		}
	}
	public ItemExpiredListener<T> getListener() {
		return listener;
	}
	public void setListener(ItemExpiredListener<T> listener) {
		this.listener = listener;
	}
}
