package fr.openwide.core.etcd.cache.model.role;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNode;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class RoleEtcdCache extends AbstractEtcdCacheNode<RoleEtcdValue> {

	public RoleEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public RoleEtcdValue getValueFromCache(String key) throws EtcdServiceException {
		return getValueFromCache(key, RoleEtcdValue.class);
	}

	@Override
	public Map<String, RoleEtcdValue> getAllValues() throws EtcdServiceException {
		return getAllValues(RoleEtcdValue.class);
	}

}
