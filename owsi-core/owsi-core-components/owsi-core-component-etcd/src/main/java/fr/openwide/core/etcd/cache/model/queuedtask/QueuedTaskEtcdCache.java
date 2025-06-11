package fr.openwide.core.etcd.cache.model.queuedtask;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNodeWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class QueuedTaskEtcdCache extends AbstractEtcdCacheNodeWithLease<QueuedTaskEtcdValue> {

	public QueuedTaskEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public QueuedTaskEtcdValue get(String key) throws EtcdServiceException {
		return get(key, QueuedTaskEtcdValue.class);
	}

	@Override
	public QueuedTaskEtcdValue remove(String key) {
		return remove(key, QueuedTaskEtcdValue.class);
	}

	@Override
	public Map<String, QueuedTaskEtcdValue> getAll() throws EtcdServiceException {
		return getAllValues(QueuedTaskEtcdValue.class);
	}

	@Override
	public QueuedTaskEtcdValue putIfAbsent(String key, QueuedTaskEtcdValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, QueuedTaskEtcdValue.class);
	}

}
