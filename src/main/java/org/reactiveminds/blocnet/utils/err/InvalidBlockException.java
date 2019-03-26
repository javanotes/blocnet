package org.reactiveminds.blocnet.utils.err;

public class InvalidBlockException extends IllegalLinkException {

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
