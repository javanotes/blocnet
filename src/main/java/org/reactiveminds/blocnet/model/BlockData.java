package org.reactiveminds.blocnet.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reactiveminds.blocnet.dto.AddRequest;
import org.reactiveminds.blocnet.dto.TxnRequest;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class BlockData implements DataSerializable{

	public BlockData() {
	}

	public BlockData(List<TxnRequest> requests) {
		this.requests.addAll(requests);
	}
	
	public List<TxnRequest> getRequests() {
		return requests;
	}

	public void setRequests(List<TxnRequest> requests) {
		this.requests = requests;
	}

	public Map<String, List<TxnRequest>> groupByChain() {
		return requests.stream().collect(Collectors.groupingBy(TxnRequest::getChain, HashMap::new, Collectors.toList()));
	}
	public String getChain() {
		return chain;
	}

	public void setChain(String chain) {
		this.chain = chain;
	}
	private List<TxnRequest> requests = new ArrayList<>();
	private String chain;
	
	public TxnRequest findTxn(String txnid) {
		return requests.stream().filter(t -> t.getTxnId().equals(txnid)).findFirst().orElse(new TxnRequest(txnid, new AddRequest()));
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(chain);
		out.writeInt(requests.size());
		for(TxnRequest t : requests) {
			t.writeData(out);
		}
			
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		setChain(in.readUTF());
		int len = in.readInt();
		if(len > 0) {
			for (int i = 0; i < len; i++) {
				TxnRequest t = new TxnRequest();
				t.readData(in);
				getRequests().add(t);
			}
		}
	}
}
