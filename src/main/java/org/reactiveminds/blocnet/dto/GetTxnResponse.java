package org.reactiveminds.blocnet.dto;

public class GetTxnResponse {

	public GetTxnResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public String getChainId() {
		return chainId;
	}
	public void setChainId(String chainId) {
		this.chainId = chainId;
	}
	public String getTxnId() {
		return txnId;
	}
	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

	String chainId;
	String txnId;
	String body;
	Response status = Response.OK;
	public Response getStatus() {
		return status;
	}

	public void setStatus(Response status) {
		this.status = status;
	}
}
