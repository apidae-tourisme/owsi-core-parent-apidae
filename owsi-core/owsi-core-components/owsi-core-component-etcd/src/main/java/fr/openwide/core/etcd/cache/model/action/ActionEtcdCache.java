package fr.openwide.core.etcd.cache.model.action;

import java.util.Map;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.AbstractEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class ActionEtcdCache extends AbstractEtcdCache<AbstractEtcdActionValue> {

	public ActionEtcdCache(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public AbstractEtcdActionValue get(String key) throws EtcdServiceException {
		return get(key, AbstractEtcdActionValue.class);
	}

	@Override
	public AbstractEtcdActionValue remove(String key) {
		return remove(key, AbstractEtcdActionValue.class);
	}

	@Override
	public Map<String, AbstractEtcdActionValue> getAll() throws EtcdServiceException {
		return getAllValues(AbstractEtcdActionValue.class);
	}

	@Override
	public AbstractEtcdActionValue putIfAbsent(String key, AbstractEtcdActionValue value) throws EtcdServiceException {
		return putIfAbsent(key, value, AbstractEtcdActionValue.class);
	}

}