package fr.openwide.core.etcd.lock.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdCache;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.exception.EtcdServiceRuntimeException;
import fr.openwide.core.etcd.common.service.AbstractEtcdClientService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdUtil;
import fr.openwide.core.etcd.lock.model.EtcdLock;
import fr.openwide.core.infinispan.model.ILockRequest;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.GetOption;

public class EtcdLockService extends AbstractEtcdClientService implements IEtcdLockService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdLockService.class);

	private static final long UNLOCK_TIMEOUT = 10;

	private static final String LOCK_PREFIX = "lock/";

	private final Map<String, ByteSequence> lockNameToKeyMap = new ConcurrentHashMap<>();

	private final LockAttributionEtcdCache lockAttributionCache;

	public EtcdLockService(EtcdClientClusterConfiguration config, LockAttributionEtcdCache lockAttributionCache) {
		super(config);
		this.lockAttributionCache = lockAttributionCache;
	}

	@Override
	public boolean tryLock(ILockRequest lockRequest) {
		String fullLockKey = getLockName(lockRequest.getLock().getKey());
		LockResponse lockResponse;
		try {
			lockResponse = lock(fullLockKey);
		} catch (EtcdServiceException e) {
			throw new EtcdServiceRuntimeException("Exception while trying to lock " + lockRequest.getLock().getKey(),
					e);
		}
		if (lockResponse != null) {
			LOGGER.debug("Lock {} acquired for key: {} by node: {}", lockResponse.getKey(), fullLockKey, getNodeName());
			lockNameToKeyMap.put(fullLockKey, lockResponse.getKey());
			try {
				lockAttributionCache.put(lockRequest.getLock().getKey(),
						LockAttributionEtcdValue.from(getNodeName(), lockRequest));
			} catch (Exception e) {
				LOGGER.error("Unable to put lock attribution item in cache for key {}", fullLockKey, e);
			}
			return true;
		}
		return false;
	}

	private LockResponse lock(String fullLockKey) throws EtcdServiceException {
		return createLockWithLease(fullLockKey, getNewLeaseIdWithKeepAlive());
	}

	private LockResponse createLockWithLease(String fullLockKey, long leaseId) throws EtcdServiceException {
		try {
			// Try to acquire the lock
			return getLockClient().lock(ByteSequence.from(fullLockKey.getBytes()), leaseId)
					.get(getClusterConfig().getLockTimeout(), TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			throw new EtcdServiceException("ExecutionException while trying to create lock", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Interrupted while trying to create lock", e);
		} catch (TimeoutException e) {
			// If lock acquisition fails, revoke the lease
			revokeLease(leaseId);
			return null;
		}
	}

	@Override
	public void unlock(String lockName) {
		try {

			String fullLockName = getLockName(lockName);
			if (lockNameToKeyMap.containsKey(fullLockName)) {
				final ByteSequence lockKey = lockNameToKeyMap.get(fullLockName);
				// unlocking in Jetcd is not blocking, still await it with a timeout to protect
				// against rare cases like network stalls or cluster issues
				getLockClient().unlock(lockKey).get(UNLOCK_TIMEOUT, TimeUnit.SECONDS);
				LOGGER.debug("Lock released for key: {} by node: {}", lockKey, getNodeName());
				lockNameToKeyMap.remove(fullLockName);
				lockAttributionCache.deleteIfNodeMatches(lockName, getNodeName());
			} else {
				LOGGER.debug("Unknown lock key for name : {}", fullLockName);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceRuntimeException("Interrupted while trying to release lock", e);
		} catch (Exception e) {
			throw new EtcdServiceRuntimeException("Exception while trying to release lock", e);
		}
	}

	@Override
	public void deleteAllLocks() throws EtcdServiceException {
		try {
			deleteAllKeysFromPrefix(getLockPrefix());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to delete all locks", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to delete all locks", e);
		}
	}

	@Override
	public Set<EtcdLock> getAllLocks() throws EtcdServiceException {
		try {
			return getAllKeysFromPrefix(getLockPrefix()).stream().map(l -> EtcdLock.from(l, extractLockKey(l)))
					.collect(Collectors.toSet());
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to retrieve all locks", e);
		}
	}

	private String getLockPrefix() {
		return LOCK_PREFIX;
	}

	private String getLockName(String key) {
		return getLockPrefix() + key;
	}

	/**
	 * Gets all locks using a jetcd transaction to simultaneously read from both
	 * lock and lock attribution caches. This ensures consistency and better
	 * performance by performing both reads in a single atomic operation.
	 */
	@Override
	public Set<EtcdLock> getLocksWithAttribution() throws EtcdServiceException {
		try {
			// Get the lock prefix (e.g., "lock/")
			String lockPrefix = getLockPrefix();
			String lockAttributionPrefix = lockAttributionCache.getCachePrefix();

			// Create transaction with two range operations to read both prefixes
			Txn txn = getKvClient().txn().Then(
					// Read all locks
					Op.get(ByteSequence.from(lockPrefix, StandardCharsets.UTF_8),
							GetOption.builder().withRange(EtcdUtil.prefixEndOf(lockPrefix)).build()),
					// Read all lock attributions
					Op.get(ByteSequence.from(lockAttributionPrefix, StandardCharsets.UTF_8),
							GetOption.builder().withRange(EtcdUtil.prefixEndOf(lockAttributionPrefix)).build()));

			TxnResponse txnResponse = txn.commit().get(getClusterConfig().getTimeout(), TimeUnit.SECONDS);

			// Extract locks from the first get response
			List<KeyValue> lockKeyValues = txnResponse.getGetResponses().get(0).getKvs();
			Set<EtcdLock> locks = lockKeyValues.stream().map(kv -> {
				String fullKey = new String(kv.getKey().getBytes(), StandardCharsets.UTF_8);
				String lockKey = extractLockKey(fullKey);
				return EtcdLock.from(fullKey, lockKey);
			}).collect(Collectors.toSet());

			// Extract lock attributions from the second get response and fill EtcdLock
			// attributions informations.
			List<KeyValue> attributionKeyValues = txnResponse.getGetResponses().get(1).getKvs();
			for (KeyValue kv : attributionKeyValues) {
				try {
					String attributionKey = new String(kv.getKey().getBytes(), StandardCharsets.UTF_8);
					String lockKey = attributionKey.substring(lockAttributionPrefix.length());
					final Optional<EtcdLock> findAny = locks.stream().filter(l -> l.getKey().equals(lockKey)).findAny();
					if (findAny.isPresent()) {
						findAny.get().setLockAttributionEtcdValue(
								lockAttributionCache.deserializeObject(kv.getValue().getBytes()));
					}
				} catch (Exception e) {
					LOGGER.warn("Failed to deserialize lock attribution for key: {}",
							new String(kv.getKey().getBytes(), StandardCharsets.UTF_8), e);
				}
			}

			return locks;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Interrupted while retrieving locks with transaction", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to retrieve locks with transaction", e);
		}
	}

	/**
	 * Extracts the lock key from the full etcd key by removing the lock prefix.
	 */
	private String extractLockKey(String fullKey) {
		if (fullKey.startsWith(getLockPrefix())) {
			return StringUtils.substringBefore(StringUtils.removeStart(fullKey, getLockPrefix()), "/");
		}
		return StringUtils.substringBefore(fullKey, "/");
	}

}