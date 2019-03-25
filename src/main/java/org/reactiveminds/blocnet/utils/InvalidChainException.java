package org.reactiveminds.blocnet.utils;

public class InvalidChainException extends LinkageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8424998415511367905L;

	public InvalidChainException() {
	}

	public InvalidChainException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidChainException(String string) {
		super(string);
	}

}
