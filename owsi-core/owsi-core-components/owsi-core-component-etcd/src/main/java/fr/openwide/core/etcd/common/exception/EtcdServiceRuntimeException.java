package fr.openwide.core.etcd.common.exception;

public class EtcdServiceRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -2124344100777945254L;

	public EtcdServiceRuntimeException(String message) {
		super(message);
	}

	public EtcdServiceRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public EtcdServiceRuntimeException(Throwable cause) {
		super(cause);
	}

}
