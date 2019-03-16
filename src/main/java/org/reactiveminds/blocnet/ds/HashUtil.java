package org.reactiveminds.blocnet.ds;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class HashUtil {
	
	public static final char[] HEX_CHARS =
		{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	/**
	 * 
	 * @param number
	 * @param repeat
	 * @return
	 */
	public static String toRepeatingIntString(int number, int repeat) {
		return IntStream.iterate(number, i -> i).limit(repeat).mapToObj(i -> ""+i).collect(Collectors.joining(""));
	}
	
	public static final String GENESIS_PREV_HASH = toRepeatingIntString(0, 64);
	
	private static String encodeHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
        
	}
	private static final ThreadLocal<MessageDigest> digestors = ThreadLocal.withInitial(() -> {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new UnsupportedOperationException("Hashing algorithm not found", e);
		}
	});
	private static Random nonceGen = new Random();
	private static byte[] toBytes(long l) {
		return ByteBuffer.allocate(8).putLong(l).array();
	}
	private static String getHash(Node block, boolean generateNonce) {
		return getHash(block, block.getPreviousHash(), generateNonce);
	}
	private static String getHash(Node block, String prevHash, boolean generateNonce) {
		Assert.isTrue(StringUtils.hasText(block.getPreviousHash()), "Prev hash is blank or null");
		MessageDigest digestor = digestors.get();
		digestor.reset();
		digestor.update(block.getData().getBytes(StandardCharsets.UTF_8));
		digestor.update(prevHash.getBytes(StandardCharsets.UTF_8));
		if (generateNonce) {
			block.setNonce(Math.abs(nonceGen.nextLong()));
		}
		digestor.update(toBytes(block.getNonce()));
		return encodeHex(digestor.digest());
	}
	/**
	 * Simple hashing with no challenge. Can be used for use case PoC
	 * @param block
	 */
	public static void generateHash(Node block) {
		String hash = getHash(block, true);
		block.setHash(hash);
	}
	/**
	 * Challenge with a prefix string. For e.g '0000' - implies the hash must start with (thus, at least have) four 0's in the beginning.
	 * WARNING : this can be computation intensive (as expected for a true blockchain)
	 * @param block
	 * @param challenge
	 * @param maxIterations
	 */
	public static boolean generateHash(Node block, String challenge, TimeCheckBean timer) {
		boolean found = false;
		String hash;
		do {
			hash = getHash(block, true);
			found = hash.startsWith(challenge);
		} while (!found && !timer.isTimeout());
		if(found)
			block.setHash(hash);
		
		return found;
	}
	/**
	 * Verify if the candidate entry is correct by computing the hash using their nonce.
	 * @param candidate
	 * @param voter
	 * @return
	 */
	public static boolean matches(Node candidate, Node voter) {
		voter.setNonce(candidate.getNonce());
		String hash = getHash(voter, false);
		return hash.equals(candidate.getHash());
	}
	/**
	 * Checks if this block is valid by recomputing the hash and matching.
	 * @param b
	 * @return
	 */
	public static boolean isValid(Node b) {
		String hash = getHash(b, false);
		return hash.equals(b.getHash());
		
	}
	/**
	 * Checks if this block is valid with a given prevHash.
	 * @param b
	 * @param prevHash
	 * @return
	 */
	public static boolean isValid(Node b, String prevHash) {
		String hash = getHash(b, prevHash, false);
		return hash.equals(b.getHash());
		
	}
}
