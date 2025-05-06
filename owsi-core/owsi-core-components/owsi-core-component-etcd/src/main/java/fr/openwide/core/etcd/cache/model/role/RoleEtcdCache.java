package fr.openwide.core.etcd.cache.model.role;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNodeWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class RoleEtcdCache extends AbstractEtcdCacheNodeWithLease<RoleEtcdValue> {

	public RoleEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public RoleEtcdValue get(String key) throws EtcdServiceException {
		return get(key, RoleEtcdValue.class);
	}

	@Override
	public RoleEtcdValue remove(String key) {
		return remove(key, RoleEtcdValue.class);
	}

	@Override
	public Map<String, RoleEtcdValue> getAll() throws EtcdServiceException {
		return getAllValues(RoleEtcdValue.class);
	}

	@Override
	public RoleEtcdValue putIfAbsent(String key, RoleEtcdValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, RoleEtcdValue.class);
	}

}
