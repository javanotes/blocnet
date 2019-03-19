package org.reactiveminds.blocnet.model;

import java.io.IOException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
@Entity
@Table(name = "blockrefs", uniqueConstraints = {@UniqueConstraint(columnNames = {"chain","txnid"})})
public class BlockRef implements DataSerializable{

	public BlockRef() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
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
	@Column(name = "chain")
	private String chain;
	@Column(name = "txnid")
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
