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

}
