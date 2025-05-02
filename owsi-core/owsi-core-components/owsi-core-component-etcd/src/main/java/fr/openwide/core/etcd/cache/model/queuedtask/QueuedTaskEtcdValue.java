package fr.openwide.core.etcd.cache.model.queuedtask;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;

public class QueuedTaskEtcdValue extends AbstractEtcdCacheValue {

	private static final long serialVersionUID = -5549707966478459308L;

	private QueuedTaskEtcdValue(Instant attributionInstant) {
		super(attributionInstant);
	}

	public static final QueuedTaskEtcdValue from(Date attributionDate) {
		return new QueuedTaskEtcdValue(attributionDate.toInstant());
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
