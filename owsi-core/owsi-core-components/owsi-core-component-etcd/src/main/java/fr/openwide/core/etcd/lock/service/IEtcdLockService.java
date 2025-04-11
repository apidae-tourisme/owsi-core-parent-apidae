package fr.openwide.core.etcd.lock.service;

import java.util.Set;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.lock.model.EtcdLock;
import fr.openwide.core.infinispan.model.ILockRequest;

/**
 * Interface for etcd distributed lock service. Provides methods for acquiring
 * and releasing distributed locks.
 */
public interface IEtcdLockService {

	/**
	 * Attempts to acquire a distributed lock for the given key.
	 */
	boolean tryLock(ILockRequest lockRequest);

	/**
	 * Releases a previously acquired distributed lock.
	 *
	 * @param lockKey the name key to unlock
	 */
	void unlock(String lockName);

	void deleteAllLocks() throws EtcdServiceException;

	/**
	 * Gets all lock currently present in etcd.
	 * 
	 * @return a set of EtcdLock
	 * @throws EtcdServiceException if an error occurs while retrieving locks
	 */
	Set<EtcdLock> getAllLocks() throws EtcdServiceException;

	/**
	 * Gets all locks using a jetcd transaction to simultaneously read from both
	 * lock and lock attribution caches. This ensures consistency and better
	 * performance by performing both reads in a single atomic operation.
	 */
	Set<EtcdLock> getLocksWithAttribution() throws EtcdServiceException;

}