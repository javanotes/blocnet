package org.reactiveminds.blocnet.model;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class BlockRef implements DataSerializable{

	public BlockRef() {
	}

	public String getChain() {
		return chain;
	}
	public void setChain(String chain) {
		this.chain = chain;
	}
	public String getTxnid() {
		return txnid;
	}
	public void setTxnid(String txnid) {
		this.txnid = txnid;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}

	private String chain;
	private String txnid;
	private String hash;
	
	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(hash);
		out.writeUTF(chain);
		out.writeUTF(txnid);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		setHash(in.readUTF());
		setChain(in.readUTF());
		setTxnid(in.readUTF());
	}
}
