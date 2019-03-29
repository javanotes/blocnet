package org.reactiveminds.blocnet.utils.err;

public class FileAccessException extends IllegalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2367149796796002857L;

	public FileAccessException(String message) {
		super(message);
	}

	public FileAccessException(String message, Exception cause) {
		super(message, cause);
	}

}
