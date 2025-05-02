package fr.openwide.core.etcd.cache.model;

import java.util.concurrent.CompletableFuture;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.PutOption;

public abstract class AbstractEtcdCacheNodeWithLease<T extends IEtcdCacheWithLeaseValue>
		extends AbstractEtcdCacheNode<T> {

	protected AbstractEtcdCacheNodeWithLease(String cacheName, EtcdClientClusterConfiguration config) {
		super(cacheName, config);
	}

	/**
	 * Stores a value in the etcd cache with a lease.
	 * The value is associated with the given key and will be automatically removed when the lease expires.
	 * 
	 * @param key The key under which to store the value
	 * @param value The value to store in the cache, which must implement {@link IEtcdCacheWithLeaseValue}
	 * @throws EtcdServiceException if there is an error storing the value in etcd
	 * @throws InterruptedException if the operation is interrupted
	 */
	@Override
	public void put(String key, T value) throws EtcdServiceException {
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
