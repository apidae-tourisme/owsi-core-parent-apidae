package fr.openwide.core.etcd.cache.model.node;

import java.util.Map;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheNodeWithLease;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class NodeEtcdCache extends AbstractEtcdCacheNodeWithLease<NodeEtcdValue> {

	public NodeEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public NodeEtcdValue get(String key) throws EtcdServiceException {
		return get(key, NodeEtcdValue.class);
	}

	@Override
	public NodeEtcdValue remove(String key) {
		return remove(key, NodeEtcdValue.class);
	}

	@Override
	public Map<String, NodeEtcdValue> getAll() throws EtcdServiceException {
		return getAllValues(NodeEtcdValue.class);
	}

	@Override
	public NodeEtcdValue putIfAbsent(String key, NodeEtcdValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, NodeEtcdValue.class);
	}

}
