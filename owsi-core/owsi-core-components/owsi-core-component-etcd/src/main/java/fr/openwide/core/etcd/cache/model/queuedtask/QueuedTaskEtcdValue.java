package fr.openwide.core.etcd.cache.model.queuedtask;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

public class QueuedTaskEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -5549707966478459308L;

	private String nodeName;

	private QueuedTaskEtcdValue(Instant attributionInstant, String nodeName) {
		super(attributionInstant);
		this.nodeName = nodeName;
	}

	public static final QueuedTaskEtcdValue from(Date attributionDate, String nodeName) {
		return new QueuedTaskEtcdValue(attributionDate.toInstant(), nodeName);

	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof QueuedTaskEtcdValue)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		return result;
	}

}
