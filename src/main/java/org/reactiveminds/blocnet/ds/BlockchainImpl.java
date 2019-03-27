package org.reactiveminds.blocnet.ds;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactiveminds.blocnet.core.BaseConfig;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.utils.Crypto;
import org.reactiveminds.blocnet.utils.err.InvalidBlockException;
import org.reactiveminds.blocnet.utils.err.InvalidChainException;
import org.springframework.util.Assert;
/**
 * A two way linked list of {@link Node}. This class is thread safe.
 * @author Sutanu_Dalui
 *
 */
class BlockchainImpl implements Blockchain {
	@Override
	public String toString() {
		return "Blockchain [chainName=" + chainName + ", blocks=" + print() + "]";
	}
	private static final byte[] GENESIS = new byte[0];//"$genesis".getBytes(StandardCharsets.UTF_8);
	private final String chainName;
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#getChainName()
	 */
	@Override
	public String getChainName() {
		return chainName;
	}
	private String challenge;
	/**
	 * Create a new chain
	 * @param name
	 * @param challengeLevel
	 */
	BlockchainImpl(String name, int challengeLevel) {
		this(name, challengeLevel, new Node(GENESIS));
	}
	/**
	 * Constructor to build an existing chain
	 * @param name
	 * @param challengeLevel
	 * @param blocks
	 */
	BlockchainImpl(String name, int challengeLevel, Deque<Block> blocks) {
		this(name, challengeLevel);
		build(blocks);
	}
	
	private void build(Deque<Block> blocks) {
		Block b = blocks.poll();
		//the first should be a genesis block
		Assert.isTrue(Crypto.GENESIS_PREV_HASH.equals(b.getPrevHash()), "First block is not a genesis block");
		genesis = Blockchain.transform(b);
		tail = genesis;
		Node curr;
		while((b = blocks.poll()) != null) {
			curr = Blockchain.transform(b);
			curr.setPrevious(tail);
			tail.setNext(curr);
			tail = curr;
		}
	}
	/**
	 * Create the 'default' chain
	 * @param challengeLevel
	 */
	BlockchainImpl(int challengeLevel) {
		this("default", challengeLevel);
	}
	private BlockchainImpl(String name, int challengeLevel, Node genesis) {
		this(name, Crypto.toRepeatingIntString(0, challengeLevel), genesis);
	}
	/**
	 * Constructor to create a new chain
	 * @param name
	 * @param challenge
	 * @param genesis
	 */
	private BlockchainImpl(String name, String challenge, Node genesis) {
		super();
		Assert.isTrue(Crypto.GENESIS_PREV_HASH.equals(genesis.getPreviousHash()), "First block is not a genesis block");
		chainName = name;
		this.genesis = genesis;
		this.challenge = challenge;
		Crypto.generateHash(genesis, challenge, BaseConfig.newTimeCheckBean());
		tail = genesis;
	}
	
	private Node genesis;
	private volatile Node tail;
	private volatile boolean discarded;
	
	private AtomicInteger size = new AtomicInteger();
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#getSize()
	 */
	@Override
	public int getSize() {
		return size.get();
	}
	private int maxSize = Integer.MAX_VALUE;
	private void checkIfDiscarded() {
		if(discarded)
			throw new IllegalStateException("Operation not allowed on discareded chain fragment");
		if(size.get() == maxSize)
			throw new IllegalStateException("Max size reached");
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#mine(java.lang.String)
	 */
	@Override
	public Node mine(byte[] data) throws TimeoutException {
		Node b = new Node(data);
		b.setPrevious(tail);
		boolean done = Crypto.generateHash(b, challenge, BaseConfig.newTimeCheckBean());
		if(!done)
			throw new TimeoutException("Mining unsuccessful. Max iteration exceeded - "+BaseConfig.newTimeCheckBean());
		
		return b;
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#append(org.reactiveminds.hazelblock.ds.Block)
	 */
	@Override
	public synchronized Blockchain append(Node next) {
		checkIfDiscarded();
		boolean valid = Crypto.isValid(next, tail.getHash());
		if(!valid)
			throw new InvalidBlockException("Not a next linked block. mineBlock(..) again");
		
		tail.setNext(next);
		tail = next;
		size.incrementAndGet();
		
		return this;
	}
	private String print() {
		Node b = genesis;
		StringBuilder s = new StringBuilder("\n");
		while (b != null) {
			s.append("  ").append(b).append("\n");
			b = b.getNext();
		}
		
		return s.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#verify()
	 */
	@Override
	public boolean verify() {
		Node b = genesis;
		while (b != null) {
			if(!Crypto.isValid(b))
				throw new InvalidChainException(chainName);
			
			b = b.getNext();
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#getMaxSize()
	 */
	@Override
	public int getMaxSize() {
		return maxSize;
	}
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	@Override
	public Iterator<Block> iterator() {
		
		return new ChainIterator();
	}
	
	private class ChainIterator implements Iterator<Block>{
		private ChainIterator() {
			super();
			this.root = genesis;
		}

		private Node root;
		@Override
		public boolean hasNext() {
			return root != null;
		}

		@Override
		public Block next() {
			if(root == null)
				throw new NoSuchElementException();
			
			Node bloc = root;
			root = bloc.getNext();
			return Blockchain.transform(bloc, getChainName());
		}
		
	}

	@Override
	public boolean isFull() {
		return getSize() == getMaxSize();
	}
	@Override
	public Block getLast() {
		return Blockchain.transform(tail, getChainName());
	}
}
