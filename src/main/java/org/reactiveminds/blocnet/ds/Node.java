package org.reactiveminds.blocnet.ds;

import java.util.Optional;

import org.reactiveminds.blocnet.utils.Crypto;
/**
 * The block node
 * @author Sutanu_Dalui
 *
 */
public class Node {

	@Override
	public String toString() {
		return "Block [data=" + data + ", nonce=" + nonce + ", timstamp=" + timstamp + ", hash=" + hash + ", previous="
				+ getPreviousHash() + "]";
	}
	Node() {
	}
	Node(byte[] data) {
		this.data = data;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public long getNonce() {
		return nonce;
	}
	public void setNonce(long nonce) {
		this.nonce = nonce;
	}
	public long getTimstamp() {
		return timstamp;
	}
	public void setTimstamp(long timstamp) {
		this.timstamp = timstamp;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public Node getPrevious() {
		return previous;
	}
	public String getPreviousHash() {
		return Optional.ofNullable(previous).map(b -> b.hash).orElse(Crypto.GENESIS_PREV_HASH);
	}
	public void setPrevious(Node previous) {
		this.previous = previous;
	}
	public Node getNext() {
		return next;
	}
	public void setNext(Node next) {
		this.next = next;
	}
	private byte[] data;
	private long nonce;
	private long timstamp = System.currentTimeMillis();
	private String hash;
	
	private Node previous;
	private Node next;
	
}
