package org.reactiveminds.blocnet.core;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.reactiveminds.blocnet.api.BlocCommitListener;
import org.reactiveminds.blocnet.model.BlockData;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class BlocCommitListeners implements MessageListener<BlockData> {

	private List<BlocCommitListener> listeners = Collections.synchronizedList(new LinkedList<>());

	public void addListener(BlocCommitListener l) {
		listeners.add(l);
	}
	@Override
	public void onMessage(Message<BlockData> message) {
		synchronized (listeners) {
			listeners.forEach(l -> l.onCommit(message.getMessageObject()));
		}
	}

}
