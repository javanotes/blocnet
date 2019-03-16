package org.reactiveminds.blocnet.dto;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class AddRequest implements DataSerializable{
	public AddRequest() {
	}
	
	public AddRequest(String payloadJson, String chainId) {
		super();
		this.payloadJson = payloadJson;
		this.chainId = chainId;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public String getChainId() {
		return chainId;
	}

	public void setChainId(String chainId) {
		this.chainId = chainId;
	}

	private String payloadJson;
	private String chainId;
	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(chainId);
		out.writeUTF(payloadJson);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		setChainId(in.readUTF());
		setPayloadJson(in.readUTF());
	}
}
