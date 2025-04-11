package fr.openwide.core.etcd.lock.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.AbstractEtcdClientService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.lock.LockResponse;

public class EtcdLockService extends AbstractEtcdClientService implements IEtcdLockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdLockService.class);

	private static final long UNLOCK_TIMEOUT = 10;

	private static final String LOCK_PREFIX = "lock/";

	private final Map<String, ByteSequence> lockNameToKeyMap = new ConcurrentHashMap<>();

	public EtcdLockService(EtcdClientClusterConfiguration config) {
		super(config);
    }

	@Override
	public boolean tryLock(String lockName) throws EtcdServiceException {
		String fullLockKey = getLockName(lockName);
		LockResponse lockResponse = lock(fullLockKey);
		if (lockResponse != null) {
			LOGGER.debug("Lock {} acquired for key: {} by node: {}", lockResponse.getKey(), fullLockKey, getNodeName());
			lockNameToKeyMap.put(fullLockKey, lockResponse.getKey());
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
					.get(getClusterConfig().getLockTimeout(),
					TimeUnit.SECONDS);
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
	public void unlock(String lockName) throws EtcdServiceException {
        try {

			String fullLockName = getLockName(lockName);
			if (lockNameToKeyMap.containsKey(fullLockName)) {
				final ByteSequence lockKey = lockNameToKeyMap.get(fullLockName);
				// unlocking in Jetcd is not blocking, still await it with a timeout to protect
				// against rare cases like network stalls or cluster issues
				getLockClient().unlock(lockKey).get(UNLOCK_TIMEOUT, TimeUnit.SECONDS);
				LOGGER.debug("Lock released for key: {} by node: {}", lockKey, getNodeName());
			} else {
				LOGGER.debug("Unknown lock key for name : {}", fullLockName);
			}
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
			throw new EtcdServiceException("Interrupted while trying to release lock", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Exception while trying to release lock", e);
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

	private String getLockPrefix() {
		return LOCK_PREFIX;
	}

	private String getLockName(String key) {
		return getLockPrefix() + key;
	}

} 