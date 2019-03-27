package org.reactiveminds.blocnet.ds;

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
	final static Blockchain NIL = new BlockchainImpl(0) {
		public int getSize() {
			return -1;
		}
	};
	public static boolean isEmptyChain(Blockchain b) {
		return b == NIL;
	}
	public static Blockchain emptyChain() {
		return NIL;
	}
	public static Node transform(Block bloc) {
		Node block = new Node();
		block.setData(bloc.getPayload());
		block.setHash(bloc.getCurrHash());
		block.setNonce(bloc.getNonce());
		block.setTimstamp(bloc.getTimestamp());
		return block;
	}
	public static Block transform(Node bloc, String chainName) {
		Block block = new Block();
		block.setCurrHash(bloc.getHash());
		block.setNonce(bloc.getNonce());
		block.setPayload(bloc.getData());
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
	Node mine(byte[] data) throws TimeoutException;

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