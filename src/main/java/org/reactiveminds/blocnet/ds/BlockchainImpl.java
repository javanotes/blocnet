package org.reactiveminds.blocnet.ds;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactiveminds.blocnet.Server;
import org.reactiveminds.blocnet.model.Block;
import org.reactiveminds.blocnet.utils.InvalidBlockException;
import org.reactiveminds.blocnet.utils.InvalidChainException;
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
	private static final String GENESIS = "$genesis";
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
	 * 
	 * @param name
	 * @param challengeLevel
	 */
	BlockchainImpl(String name, int challengeLevel) {
		this(name, challengeLevel, new Node(GENESIS));
	}
	/**
	 * 
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
		genesis = transform(b);
		Node prev = genesis;
		Node curr;
		while((b = blocks.poll()) != null) {
			curr = transform(b);
			curr.setPrevious(prev);
			prev.setNext(curr);
			prev = curr;
		}
	}
	/**
	 * 
	 * @param challengeLevel
	 */
	BlockchainImpl(int challengeLevel) {
		this(GENESIS, challengeLevel);
	}
	private BlockchainImpl(String name, int challengeLevel, Node genesis) {
		this(name, HashUtil.toRepeatingIntString(0, challengeLevel), genesis);
	}
	private BlockchainImpl(String name, String challenge, Node genesis) {
		super();
		chainName = name;
		this.genesis = genesis;
		this.challenge = challenge;
		HashUtil.generateHash(genesis, challenge, Server.newTimeCheckBean());
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
	public Node mine(String data) throws TimeoutException {
		Node b = new Node(data);
		b.setPrevious(tail);
		boolean done = HashUtil.generateHash(b, challenge, Server.newTimeCheckBean());
		if(!done)
			throw new TimeoutException("append failed! Max iteration exceeded - "+Server.newTimeCheckBean());
		
		return b;
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#append(org.reactiveminds.hazelblock.ds.Block)
	 */
	@Override
	public synchronized Blockchain append(Node next) {
		checkIfDiscarded();
		boolean valid = HashUtil.isValid(next, tail.getHash());
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
	public String toJson() {
		return "";
		
	}
	/* (non-Javadoc)
	 * @see org.reactiveminds.hazelblock.ds.IBlockchain#verify()
	 */
	@Override
	public boolean verify() {
		Node b = genesis;
		while (b != null) {
			if(!HashUtil.isValid(b))
				throw new InvalidChainException(chainName);
			
			b = b.getNext();
		}
		return true;
	}
	/*@Deprecated
	private void mess_it() {
		tail.setData("changed");
	}
	
	public static void main(String[] args) {
		BlockchainImpl b = new BlockchainImpl("TRANS_BLOC", 4);
		Node bloc = b.mine("Tom");
		b.append(bloc);
		bloc = b.mine("owes 36");
		b.append(bloc);
		bloc = b.mine("from Dick");
		b.append(bloc);
		
		System.out.println(b);
		System.out.println("is valid chain ? "+b.verify());
		b.mess_it();
		System.out.println("is valid chain ? "+b.verify());
	}*/
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
			return transform(bloc);
		}
		
	}

	private Block transform(Node bloc) {
		Block block = new Block();
		block.setCurrHash(bloc.getHash());
		block.setNonce(bloc.getNonce());
		block.setPayload(bloc.getData().getBytes(StandardCharsets.UTF_8));
		block.setPrevHash(bloc.getPreviousHash());
		block.setTimestamp(bloc.getTimstamp());
		block.setChain(getChainName());
		return block;
	}
	private Node transform(Block bloc) {
		Node block = new Node();
		block.setData(new String(bloc.getPayload(), StandardCharsets.UTF_8));
		block.setHash(bloc.getCurrHash());
		block.setNonce(bloc.getNonce());
		block.setTimstamp(bloc.getTimestamp());
		return block;
	}
	@Override
	public boolean isFull() {
		return getSize() == getMaxSize();
	}
	@Override
	public Block getLast() {
		return transform(tail);
	}
}
