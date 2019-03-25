package org.reactiveminds.blocnet.ds;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.concurrent.TimeoutException;

import org.reactiveminds.blocnet.model.Block;


/**
 * The core api for creating blockchain data structure.
 * @author Sutanu_Dalui
 *
 */
public interface Blockchain extends Iterable<Block>{
	public static Blockchain newInstance(String name, int challengeLevel) {
		return new BlockchainImpl(name, challengeLevel);
	}
	public static Blockchain buildInstance(String name, int challengeLevel, Deque<Block> blocks) {
		return new BlockchainImpl(name, challengeLevel, blocks);
	}
	public static Blockchain newInstance(int challengeLevel) {
		return new BlockchainImpl(challengeLevel);
	}
	public static Blockchain emptyChain() {
		return new BlockchainImpl(0) {
			public int getSize() {
				return -1;
			}
		};
	}
	public static Node transform(Block bloc) {
		Node block = new Node();
		block.setData(new String(bloc.getPayload(), StandardCharsets.UTF_8));
		block.setHash(bloc.getCurrHash());
		block.setNonce(bloc.getNonce());
		block.setTimstamp(bloc.getTimestamp());
		return block;
	}
	public static Block transform(Node bloc, String chainName) {
		Block block = new Block();
		block.setCurrHash(bloc.getHash());
		block.setNonce(bloc.getNonce());
		block.setPayload(bloc.getData().getBytes(StandardCharsets.UTF_8));
		block.setPrevHash(bloc.getPreviousHash());
		block.setTimestamp(bloc.getTimstamp());
		block.setChain(chainName);
		return block;
	}
	/**
	 * Name of this chain
	 * @return
	 */
	String getChainName();
	/**
	 * The current size of this chain
	 * @return
	 */
	int getSize();

	/**
	 * Mine the next block of data
	 * @param data
	 * @return
	 * @throws TimeoutException 
	 */
	Node mine(String data) throws TimeoutException;

	/**
	 * Append next block
	 * @param next
	 * @return
	 */
	Blockchain append(Node next);
	/**
	 * Get the last block
	 * @return
	 */
	Block getLast();
	/**
	 * Verify validity of this chain
	 * @return
	 */
	boolean verify();
	/**
	 * Max size for this chain
	 * @return
	 */
	int getMaxSize();
	/**
	 * 
	 * @return
	 */
	boolean isFull();
}