package fr.openwide.core.etcd.cache.model.node;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class NodeEtcdCache extends AbstractEtcdCacheWithLease<NodeEtcdValue> {

	public NodeEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public NodeEtcdValue getValueFromCache(String key) throws EtcdServiceException {
		return getValueFromCache(key, NodeEtcdValue.class);
	}

	@Override
	public Map<String, NodeEtcdValue> getAllValues() throws EtcdServiceException {
		return getAllValues(NodeEtcdValue.class);
	}

}
