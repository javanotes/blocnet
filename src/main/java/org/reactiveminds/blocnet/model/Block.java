package org.reactiveminds.blocnet.model;

import java.io.IOException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
@Entity
@Table(name = "blockchains", uniqueConstraints = {@UniqueConstraint(columnNames = {"chain","curr_hash"})})
public class Block implements DataSerializable{
	@Override
	public String toString() {
		return "Block [id=" + id + ", chain=" + chain + ", timestamp=" + timestamp + ", nonce=" + nonce + ", prevHash="
				+ prevHash + ", currHash=" + currHash + "]";
	}
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
	public Block() {
	}
	/*public Block(Entities pool, String prevHash) {
		this.timestamp = System.currentTimeMillis();
		this.payload = SerdeUtil.toBytes(pool);
		this.prevHash = prevHash;
	}*/
	private String chain;
	private long timestamp;
	private long nonce;
	@Column(name = "prev_hash")
	private String prevHash;
	@Column(name = "curr_hash")
	private String currHash;
	
	public String getPrevHash() {
		return prevHash;
	}
	public void setPrevHash(String prevHash) {
		this.prevHash = prevHash;
	}
	public String getCurrHash() {
		return currHash;
	}
	public void setCurrHash(String currHash) {
		this.currHash = currHash;
	}
	
	@Lob
	private byte[] payload;
	public long getNonce() {
		return nonce;
	}
	public void setNonce(long nonce) {
		this.nonce = nonce;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeLong(nonce);
		out.writeLong(timestamp);
		out.writeUTF(prevHash);
		out.writeUTF(currHash);
		out.writeUTF(chain);
		out.write(payload);
	}
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		setNonce(in.readLong());
		setTimestamp(in.readLong());
		setPrevHash(in.readUTF());
		setCurrHash(in.readUTF());
		setChain(in.readUTF());
		setPayload(in.readByteArray());
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getChain() {
		return chain;
	}
	public void setChain(String chain) {
		this.chain = chain;
	}
}
