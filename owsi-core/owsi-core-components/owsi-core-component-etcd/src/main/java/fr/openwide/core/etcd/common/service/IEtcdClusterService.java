package fr.openwide.core.etcd.common.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.action.model.role.SwitchRoleResultActionResult;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdValue;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdValue;
import fr.openwide.core.etcd.cache.model.queuedtask.QueuedTaskEtcdValue;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdValue;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdValue;
import fr.openwide.core.etcd.cache.service.IEtcdCacheManager;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.lock.model.EtcdLock;
import fr.openwide.core.etcd.lock.service.IEtcdLockService;
import fr.openwide.core.infinispan.model.DoIfRoleWithLock;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IRole;

public interface IEtcdClusterService extends AutoCloseable {

	void init();

	void stop();

	DoIfRoleWithLock doIfRoleWithLock(ILockRequest lockRequest, Runnable runnable) throws ExecutionException;

	boolean doWithLockPriority(ILockRequest lockRequest, Runnable runnable) throws ExecutionException;

	IEtcdCacheManager getCacheManager();

	IEtcdLockService getLockService();

	void unassignRole(IRole role) throws EtcdServiceException;

	Set<IRole> getAllRolesForAssignation();

	Set<IRole> getAllRolesForRolesRequests();

	void doRebalanceRoles();

	boolean isClusterActive();

	String getNodeName();

	<T> T syncedAction(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
			throws ExecutionException, TimeoutException;

	RoleEtcdValue getRole(IRole iRole);

	void assignRole(IRole role) throws EtcdServiceException;

	SwitchRoleResultActionResult assignRole(IRole iRole, NodeEtcdValue nodeValue);

	Set<EtcdLock> getLocks();

	RoleRequestEtcdValue getRoleRequest(IRole input);

	void removeRoleRequest(IRole object);

	LockAttributionEtcdValue getLockAttribution(EtcdLock lock);

	SwitchRoleResultActionResult doReleaseRole(IRole role);

	SwitchRoleResultActionResult doCaptureRole(IRole role);

	Map<String, NodeEtcdValue> getNodes();

	Map<String, RoleEtcdValue> getRoles() throws EtcdServiceException;

	Map<String, PriorityQueueEtcdValue> getPriorityQueues();

	Map<String, QueuedTaskEtcdValue> getAllTasksFromCache();

}
