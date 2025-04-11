package fr.openwide.core.etcd.cache.service;

import fr.openwide.core.etcd.cache.model.action.ActionEtcdCache;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdCache;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdCache;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdCache;
import fr.openwide.core.etcd.cache.model.queuedtask.QueuedTaskEtcdCache;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdCache;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;

public interface IEtcdCacheManager {

	void init() throws EtcdServiceException;

	void stop() throws EtcdServiceException;

	NodeEtcdCache getNodeCache();

	QueuedTaskEtcdCache getQueuedTaskCache();

	RoleEtcdCache getRoleCache();

	RoleRequestEtcdCache getRoleRequestCache();

	ActionEtcdCache getActionCache();

	ActionEtcdCache getActionResultCache();

	PriorityQueueEtcdCache getPriorityQueueCache();

	LockAttributionEtcdCache getLockAttributionCache();

	void deleteAllCaches() throws EtcdServiceException;

}
