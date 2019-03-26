package org.reactiveminds.blocnet.core;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.reactiveminds.blocnet.ds.Blockchain;

public class CachedBlockchain implements Delayed{
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CachedBlockchain other = (CachedBlockchain) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	public long getCreated() {
		return created;
	}
	public Blockchain getItem() {
		return item;
	}
	CachedBlockchain copyEquals() {
		return new CachedBlockchain(name, created);
	}
	static CachedBlockchain withName(String name) {
		return new CachedBlockchain(name, System.currentTimeMillis());
	}
	private CachedBlockchain(String name, long created) {
		super();
		this.name = name;
		this.created = created;
	}
	public CachedBlockchain(Blockchain item) {
		super();
		this.created = System.currentTimeMillis();
		this.name = item.getChainName();
		this.item = item;
	}
	public CachedBlockchain(Blockchain item, long ttl) {
		this(item);
		this.timeToLiveMillis = ttl;
	}
	private final String name;
	private final long created;
	private long timeToLiveMillis = Long.MAX_VALUE;
	private Blockchain item;
	public void setItem(Blockchain item) {
		this.item = item;
	}
	@Override
	public int compareTo(Delayed that) {
		return Long.compare(this.getDelayMillis(), ((CachedBlockchain) that).getDelayMillis());
	}
	private long getDelayMillis() {
        return (created + timeToLiveMillis) - System.currentTimeMillis();
    }
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
	}
	
}