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
	public AbstractEtcdActionValue getValueFromCache(String key) throws EtcdServiceException {
		return getValueFromCache(key, AbstractEtcdActionValue.class);
    }

    @Override
	public Map<String, AbstractEtcdActionValue> getAllValues() throws EtcdServiceException {
		return getAllValues(AbstractEtcdActionValue.class);
    }

} 