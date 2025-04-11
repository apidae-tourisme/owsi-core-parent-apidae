package fr.openwide.core.etcd.lock.service;

import java.util.concurrent.ExecutionException;

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
	 * @throws ExecutionException if an error occurs while trying to acquire the
	 *                            lock
	 */
	boolean tryLock(String lockName) throws EtcdServiceException;

	/**
	 * Releases a previously acquired distributed lock.
	 *
	 * @param lockKey the name key to unlock
	 * @throws ExecutionException if an error occurs while trying to release the
	 *                            lock
	 */
	void unlock(String lockName) throws EtcdServiceException;

	void deleteAllLocks() throws EtcdServiceException;

}