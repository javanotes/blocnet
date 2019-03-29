package org.reactiveminds.blocnet.dto;

import java.io.IOException;
import java.io.Serializable;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class TxnRequest implements Serializable, DataSerializable, PartitionAware<String>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public String toString() {
		return "[txnId=" + txnId + ", request=" + request + ", chain=" + chain + "]";
	}

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
	
	public void setChain(String chain) {
		this.chain = chain;
	}

	private String txnId;
	private String request;
	private String chain;
	private byte[] requestRaw;
	private boolean isRaw = false;
	/**
	 * Request for string payload
	 * @param txnId
	 * @param request
	 */
	public TxnRequest(String txnId, AddRequest request) {
		super();
		this.txnId = txnId;
		this.request = request.getPayloadJson();
		this.chain = request.getChainId();
	}
	/**
	 * Request for raw bytes
	 * @param txnId
	 * @param chain
	 * @param raw
	 */
	public TxnRequest(String txnId, String chain, byte[] raw) {
		super();
		this.txnId = txnId;
		this.chain = chain;
		this.requestRaw = raw;
		this.isRaw = true;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(chain);
		out.writeUTF(txnId);
		out.writeBoolean(isRaw);
		if(isRaw)
			out.write(requestRaw);
		else
			out.writeUTF(request);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		chain = in.readUTF();
		txnId = in.readUTF();
		isRaw = in.readBoolean();
		if(isRaw)
			requestRaw = in.readByteArray();
		else
			request = in.readUTF();
	}
	public static final char KEY_SEP = '#';
	@Override
	public String getPartitionKey() {
		return getChain()+KEY_SEP+getTxnId();
	}
	public static String makePartitionKey(String chain, String txnId) {
		return chain+KEY_SEP+txnId;
	}
	public boolean isRaw() {
		return isRaw;
	}

	public byte[] getRequestRaw() {
		return requestRaw;
	}
}
