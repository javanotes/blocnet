package org.reactiveminds.blocnet.utils.err;
/**
 * Base exception for cryptographic link invalidation
 * @author Sutanu_Dalui
 *
 */
public abstract class IllegalLinkException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -356931779521559918L;

	public IllegalLinkException() {
	}

	public IllegalLinkException(String message) {
		super(message);
	}

	public IllegalLinkException(Throwable cause) {
		super(cause);
	}

	public IllegalLinkException(String message, Throwable cause) {
		super(message, cause);
	}
}
