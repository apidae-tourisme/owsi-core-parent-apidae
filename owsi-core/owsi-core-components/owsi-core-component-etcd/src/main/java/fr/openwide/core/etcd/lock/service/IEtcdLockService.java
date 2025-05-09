package fr.openwide.core.etcd.lock.service;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;

/**
 * Interface for etcd distributed lock service. Provides methods for acquiring
 * and releasing distributed locks.
 */
public interface IEtcdLockService {

	/**
	 * Attempts to acquire a distributed lock for the given key.
	 *
	 * @param lockKey the key to lock
	 * @return true if the lock was acquired successfully, false otherwise
	 */
	boolean tryLock(String lockName);

	/**
	 * Releases a previously acquired distributed lock.
	 *
	 * @param lockKey the name key to unlock
	 */
	void unlock(String lockName);

	void deleteAllLocks() throws EtcdServiceException;

}