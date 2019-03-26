package org.reactiveminds.blocnet.core;

import java.util.Set;
import java.util.Map.Entry;

import org.reactiveminds.blocnet.dto.TxnRequest;

import com.hazelcast.query.Predicate;

public interface TxnPredicate extends Predicate<String, TxnRequest>{

	static class RemoveTxnPredicate implements TxnPredicate{

		protected RemoveTxnPredicate(Set<TxnRequest> keys) {
			super();
			this.keys = keys;
		}

		private final Set<TxnRequest> keys;
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public boolean apply(Entry<String, TxnRequest> mapEntry) {
			return keys.contains(mapEntry.getValue());
		}
		
	}
	static class MatchAllTxnPredicate implements TxnPredicate{

		protected MatchAllTxnPredicate() {
			super();
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public boolean apply(Entry<String, TxnRequest> mapEntry) {
			return true;
		}
		
	}
}
