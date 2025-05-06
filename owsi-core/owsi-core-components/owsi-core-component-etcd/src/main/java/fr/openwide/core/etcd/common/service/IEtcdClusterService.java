package fr.openwide.core.etcd.common.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdValue;
import fr.openwide.core.etcd.cache.service.IEtcdCacheManager;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.model.DoIfRoleWithLock;
import fr.openwide.core.etcd.coordinator.service.IEtcdCoordinatorService;
import fr.openwide.core.etcd.lock.service.IEtcdLockService;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IRole;

public interface IEtcdClusterService extends AutoCloseable {

	void init();

	void stop();

	Set<String> getNodes();

	DoIfRoleWithLock doIfRoleWithLock(ILockRequest lockRequest, Runnable runnable) throws EtcdServiceException;

	boolean doWithLockPriority(ILockRequest lockRequest, Runnable runnable) throws ExecutionException;

	IEtcdCacheManager getCacheManager();

	IEtcdLockService getLockService();

	IEtcdCoordinatorService getCoordinatorService();

	void assignRole(IRole role) throws EtcdServiceException;

	void unassignRole(IRole role) throws EtcdServiceException;

	Map<String, RoleEtcdValue> getRoles() throws EtcdServiceException;

	Set<IRole> getAllRolesForAssignation();

	Set<IRole> getAllRolesForRolesRequests();

	void doRebalanceRoles();

	boolean isClusterActive();

	String getNodeName();

	<T> T syncedAction(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
			throws ExecutionException, TimeoutException;

}
