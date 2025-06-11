package fr.openwide.core.etcd.cache.model.priorityqueue;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNodeWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class PriorityQueueEtcdCache extends AbstractEtcdCacheNodeWithLease<PriorityQueueEtcdValue> {

    public PriorityQueueEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
        super(cacheName, config);
    }

    @Override
    public PriorityQueueEtcdValue get(String key) throws EtcdServiceException {
        return get(key, PriorityQueueEtcdValue.class);
    }

    @Override
    public PriorityQueueEtcdValue remove(String key) {
        return remove(key, PriorityQueueEtcdValue.class);
    }

    @Override
    public Map<String, PriorityQueueEtcdValue> getAll() throws EtcdServiceException {
        return getAllValues(PriorityQueueEtcdValue.class);
    }

    @Override
    public PriorityQueueEtcdValue putIfAbsent(String key, PriorityQueueEtcdValue value) throws EtcdServiceException {
        return putIfAbsent(key, value, PriorityQueueEtcdValue.class);
    }
} 