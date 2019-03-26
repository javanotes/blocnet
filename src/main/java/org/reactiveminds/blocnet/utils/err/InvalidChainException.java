package org.reactiveminds.blocnet.utils.err;

public class InvalidChainException extends IllegalLinkException {

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
