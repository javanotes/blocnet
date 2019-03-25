package org.reactiveminds.blocnet.utils;

public class InvalidBlockException extends LinkageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8424998415511367905L;

	public InvalidBlockException() {
	}

	public InvalidBlockException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidBlockException(String string) {
		super(string);
	}

}
