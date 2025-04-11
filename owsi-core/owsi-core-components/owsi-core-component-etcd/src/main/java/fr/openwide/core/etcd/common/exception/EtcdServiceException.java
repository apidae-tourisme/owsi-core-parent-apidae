package fr.openwide.core.etcd.common.exception;

public class EtcdServiceException extends Exception {

	private static final long serialVersionUID = -1468318659806179894L;

	public EtcdServiceException(String message) {
		super(message);
	}

	public EtcdServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
