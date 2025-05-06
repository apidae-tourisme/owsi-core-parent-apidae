package fr.openwide.core.etcd.cache.model.rolerequest;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNode;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class RoleRequestEtcdCache extends AbstractEtcdCacheNode<RoleRequestEtcdValue> {

	public RoleRequestEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public RoleRequestEtcdValue get(String key) throws EtcdServiceException {
		return get(key, RoleRequestEtcdValue.class);
	}

	@Override
	public RoleRequestEtcdValue remove(String key) {
		return remove(key, RoleRequestEtcdValue.class);
	}

	@Override
	public Map<String, RoleRequestEtcdValue> getAll() throws EtcdServiceException {
		return getAllValues(RoleRequestEtcdValue.class);
	}

	@Override
	public RoleRequestEtcdValue putIfAbsent(String key, RoleRequestEtcdValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, RoleRequestEtcdValue.class);
	}

}
