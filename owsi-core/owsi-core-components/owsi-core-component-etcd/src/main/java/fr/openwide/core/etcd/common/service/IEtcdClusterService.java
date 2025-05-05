package fr.openwide.core.etcd.common.service;

import java.util.Map;
import java.util.Set;

import fr.openwide.core.etcd.cache.model.role.RoleEtcdValue;
import fr.openwide.core.etcd.cache.service.EtcdCacheManager;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.model.DoIfRoleWithLock;
import fr.openwide.core.etcd.lock.service.EtcdLockService;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IRole;

public interface IEtcdClusterService extends AutoCloseable {

	void init();

	void stop();

	DoIfRoleWithLock doIfRoleWithLock(ILockRequest lockRequest, Runnable runnable) throws EtcdServiceException;

	EtcdCacheManager getCacheManager();

	EtcdLockService getLockService();

	void assignRole(IRole role) throws EtcdServiceException;

	void unassignRole(IRole role) throws EtcdServiceException;

	Map<String, RoleEtcdValue> getRoles() throws EtcdServiceException;

	Set<IRole> getAllRolesForAssignation();

	Set<IRole> getAllRolesForRolesRequests();

	void doRebalanceRoles();

	boolean isClusterActive();

	String getNodeName();

}
