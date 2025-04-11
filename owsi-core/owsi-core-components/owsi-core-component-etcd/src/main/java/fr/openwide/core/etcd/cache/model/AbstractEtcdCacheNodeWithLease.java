package fr.openwide.core.etcd.cache.model;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.exception.EtcdServiceRuntimeException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.options.PutOption;

public abstract class AbstractEtcdCacheNodeWithLease<T extends IEtcdCacheNodeValue>
		extends AbstractEtcdCacheNode<T> {

	protected AbstractEtcdCacheNodeWithLease(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	protected PutOption getPutOption() {
		try {
			return PutOption.builder().withLeaseId(getGlobalClientLeaseIdWithKeepAlive()).build();
		} catch (EtcdServiceException e) {
			throw new EtcdServiceRuntimeException(e);
		}
	}

}
