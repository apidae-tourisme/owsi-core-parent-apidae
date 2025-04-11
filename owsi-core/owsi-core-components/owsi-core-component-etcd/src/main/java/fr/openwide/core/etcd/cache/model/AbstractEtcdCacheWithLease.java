package fr.openwide.core.etcd.cache.model;

import java.util.concurrent.CompletableFuture;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.PutOption;

public abstract class AbstractEtcdCacheWithLease<T extends IEtcdCacheWithLeaseValue> extends AbstractEtcdCache<T> {

	protected AbstractEtcdCacheWithLease(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	@Override
	public void putValueInCache(String key, T value) throws EtcdServiceException {
		try {
			String prefixedKey = getCacheKey(key);
			Long leaseId = getGlobalClientLeaseIdWithKeepAlive();
			value.setLeaseId(leaseId);
			CompletableFuture<PutResponse> putFuture = getKvClient().put(
					ByteSequence.from(prefixedKey.getBytes()),
					ByteSequence.from(serializeObject(value)), PutOption.builder().withLeaseId(leaseId).build());
			putFuture.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to put value in cache '" + getCacheName() + "' for key: " + key, e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to put value in cache '" + getCacheName() + "' for key: " + key, e);
		}
	}

}
