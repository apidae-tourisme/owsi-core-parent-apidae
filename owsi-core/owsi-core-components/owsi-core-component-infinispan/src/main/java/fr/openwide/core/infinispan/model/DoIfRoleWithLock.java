package fr.openwide.core.infinispan.model;

public enum DoIfRoleWithLock {
	/**
	 * The operation was successfully executed
	 */
	RUN,

	/**
	 * The operation was not executed because the current node does not own the
	 * required role
	 */
	NOT_RUN_ROLE_NOT_OWNED,

	/**
	 * The operation was not executed because the lock could not be acquired
	 */
	NOT_RUN_LOCK_NOT_AVAILABLE,

	/**
	 * The operation was not executed because the cluster is not available
	 */
	NOT_RUN_CLUSTER_UNAVAILABLE
}
