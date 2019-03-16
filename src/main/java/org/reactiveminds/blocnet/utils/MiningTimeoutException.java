package org.reactiveminds.blocnet.utils;

import java.util.concurrent.TimeoutException;

public class MiningTimeoutException extends IllegalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MiningTimeoutException() {
	}

	public MiningTimeoutException(String s) {
		super(s);
	}

	public MiningTimeoutException(String message, TimeoutException cause) {
		super(message, cause);
	}

}
