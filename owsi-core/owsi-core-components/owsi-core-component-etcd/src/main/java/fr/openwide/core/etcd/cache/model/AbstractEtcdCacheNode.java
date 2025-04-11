package fr.openwide.core.etcd.cache.model;

import java.util.Map.Entry;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public abstract class AbstractEtcdCacheNode<T extends IEtcdCacheNodeValue> extends AbstractEtcdCache<T> {

	protected AbstractEtcdCacheNode(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	public void cleanCacheFromNode(String nodeName) throws EtcdServiceException {
		if (Strings.isNullOrEmpty(nodeName)) {
			return;
		}
		for (Entry<String, T> roleEntry : getAll().entrySet()) {
			if (roleEntry.getValue() != null && Objects.equal(nodeName, roleEntry.getValue().getNodeName())) {
				delete(roleEntry.getKey());
			}
		}
	}

	public boolean deleteIfNodeMatches(String key, String nodeName) throws EtcdServiceException {
		final T valueFromCache = get(key);
		if (valueFromCache != null && Objects.equal(nodeName, valueFromCache.getNodeName())) {
			return delete(key);
		}
		return false;

	}

}
