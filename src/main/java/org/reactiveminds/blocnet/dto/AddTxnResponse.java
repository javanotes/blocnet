package org.reactiveminds.blocnet.dto;

public class AddTxnResponse {

	public AddTxnResponse() {
	}

	public Response getStatus() {
		return status;
	}
	public void setStatus(Response status) {
		this.status = status;
	}
	public String getTxnId() {
		return txnId;
	}
	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}
	public String getChainId() {
		return chainId;
	}
	public void setChainId(String chainId) {
		this.chainId = chainId;
	}

	Response status;
	String txnId;
	String chainId;
}
