package org.reactiveminds.blocnet.dto;

public class GetRawResponse {

	public GetRawResponse() {
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
	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}

	String chainId;
	String txnId;
	byte[] body;
	Response status = Response.OK;
	public Response getStatus() {
		return status;
	}

	public void setStatus(Response status) {
		this.status = status;
	}
}
