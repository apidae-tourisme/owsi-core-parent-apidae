package fr.openwide.core.etcd.cache.model.lockattribution;

import java.io.IOException;
import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNodeWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class LockAttributionEtcdCache extends AbstractEtcdCacheNodeWithLease<LockAttributionEtcdValue> {

	public LockAttributionEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public LockAttributionEtcdValue get(String key) throws EtcdServiceException {
		return get(key, LockAttributionEtcdValue.class);
	}

	@Override
	public LockAttributionEtcdValue remove(String key) {
		return remove(key, LockAttributionEtcdValue.class);
	}

	@Override
	public Map<String, LockAttributionEtcdValue> getAll() throws EtcdServiceException {
		return getAllValues(LockAttributionEtcdValue.class);
	}

	@Override
	public LockAttributionEtcdValue putIfAbsent(String key, LockAttributionEtcdValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, LockAttributionEtcdValue.class);
	}

	public LockAttributionEtcdValue deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
		return super.deserializeObject(data, LockAttributionEtcdValue.class);
	}

}
