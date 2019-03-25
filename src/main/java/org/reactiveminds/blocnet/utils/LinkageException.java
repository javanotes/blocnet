package org.reactiveminds.blocnet.utils;

public abstract class LinkageException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -356931779521559918L;

	public LinkageException() {
	}

	public LinkageException(String message) {
		super(message);
	}

	public LinkageException(Throwable cause) {
		super(cause);
	}

	public LinkageException(String message, Throwable cause) {
		super(message, cause);
	}
}
