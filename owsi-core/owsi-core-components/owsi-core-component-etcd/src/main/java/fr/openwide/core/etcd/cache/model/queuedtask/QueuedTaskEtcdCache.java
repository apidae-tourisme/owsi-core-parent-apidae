package fr.openwide.core.etcd.cache.model.queuedtask;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class QueuedTaskEtcdCache extends AbstractEtcdCache<QueuedTaskEtcdValue> {

	public QueuedTaskEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public QueuedTaskEtcdValue getValueFromCache(String key) throws EtcdServiceException {
		return getValueFromCache(key, QueuedTaskEtcdValue.class);
	}

	@Override
	public Map<String, QueuedTaskEtcdValue> getAllValues() throws EtcdServiceException {
		return getAllValues(QueuedTaskEtcdValue.class);
	}

}
