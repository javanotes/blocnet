package org.reactiveminds.blocnet.dto;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class TxnRequest implements DataSerializable{
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chain == null) ? 0 : chain.hashCode());
		result = prime * result + ((txnId == null) ? 0 : txnId.hashCode());
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
		TxnRequest other = (TxnRequest) obj;
		if (chain == null) {
			if (other.chain != null)
				return false;
		} else if (!chain.equals(other.chain))
			return false;
		if (txnId == null) {
			if (other.txnId != null)
				return false;
		} else if (!txnId.equals(other.txnId))
			return false;
		return true;
	}

	public String getTxnId() {
		return txnId;
	}
	
	public String getRequest() {
		return request;
	}
	public String getChain() {
		return chain;
	}

	public TxnRequest() {
	}
	
	private String txnId;
	private String request;
	private String chain;
	public TxnRequest(String txnId, AddRequest request) {
		super();
		this.txnId = txnId;
		this.request = request.getPayloadJson();
		this.chain = request.getChainId();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(request);
		out.writeUTF(chain);
		out.writeUTF(txnId);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		request = in.readUTF();
		chain = in.readUTF();
		txnId = in.readUTF();
	}
	
}
