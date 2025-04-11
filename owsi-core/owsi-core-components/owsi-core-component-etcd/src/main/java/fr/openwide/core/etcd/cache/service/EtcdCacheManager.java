package fr.openwide.core.etcd.cache.service;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCache;
import fr.openwide.core.etcd.cache.model.action.ActionEtcdCache;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdCache;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdCache;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdCache;
import fr.openwide.core.etcd.cache.model.queuedtask.QueuedTaskEtcdCache;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdCache;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class EtcdCacheManager implements IEtcdCacheManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdCacheManager.class);

	private final QueuedTaskEtcdCache queuedTaskEtcdCache;
	private final RoleEtcdCache roleEtcdCache;
	private final NodeEtcdCache nodeEtcdCache;
	private final RoleRequestEtcdCache roleRequestEtcdCache;
	private final ActionEtcdCache actionEtcdCache;
	private final ActionEtcdCache actionResultEtcdCache;
	private final PriorityQueueEtcdCache priorityQueueEtcdCache;
	private final LockAttributionEtcdCache lockAttributionEtcdCache;

	private final List<AbstractEtcdCache<?>> allCaches;

	private static final String QUEUED_TASK_CACHE = "task";
	private static final String ROLE_CACHE = "role";
	private static final String NODE_CACHE = "node";
	private static final String ACTION_CACHE = "action";
	private static final String ACTION_RESULT_CACHE = "result_action";
	private static final String PRIORITY_QUEUE_CACHE = "priority_queue";
	private static final String LOCK_ATTRIBUTION_CACHE = "lock_attribution";

	public EtcdCacheManager(EtcdClientClusterConfiguration config) {
		queuedTaskEtcdCache = new QueuedTaskEtcdCache(QUEUED_TASK_CACHE, config);
		roleEtcdCache = new RoleEtcdCache(ROLE_CACHE, config);
		nodeEtcdCache = new NodeEtcdCache(NODE_CACHE, config);
		roleRequestEtcdCache = new RoleRequestEtcdCache(NODE_CACHE, config);
		actionEtcdCache = new ActionEtcdCache(ACTION_CACHE, config);
		actionResultEtcdCache = new ActionEtcdCache(ACTION_RESULT_CACHE, config);
		priorityQueueEtcdCache = new PriorityQueueEtcdCache(PRIORITY_QUEUE_CACHE, config);
		lockAttributionEtcdCache = new LockAttributionEtcdCache(LOCK_ATTRIBUTION_CACHE, config);
		allCaches = List.of(queuedTaskEtcdCache, roleEtcdCache, nodeEtcdCache, roleRequestEtcdCache, actionEtcdCache,
				actionResultEtcdCache, priorityQueueEtcdCache, lockAttributionEtcdCache);
	}

	@Override
	public void init() throws EtcdServiceException {
		LOGGER.debug("Starting cacheManager");
		for (AbstractEtcdCache<?> c : allCaches) {
			initCache(c);
		}
		LOGGER.debug("cacheManager started");
	}

	private void initCache(AbstractEtcdCache<?> c) throws EtcdServiceException {
		LOGGER.debug("Starting caches {}", c.getCacheName());
		c.ensureCacheExists();
	}

	@Override
	public void stop() throws EtcdServiceException {
		// Nothing to do
	}

	@Override
	public QueuedTaskEtcdCache getQueuedTaskCache() {
		return queuedTaskEtcdCache;
	}

	@Override
	public RoleEtcdCache getRoleCache() {
		return roleEtcdCache;
	}

	@Override
	public NodeEtcdCache getNodeCache() {
		return nodeEtcdCache;
	}

	@Override
	public RoleRequestEtcdCache getRoleRequestCache() {
		return roleRequestEtcdCache;
	}

	@Override
	public ActionEtcdCache getActionCache() {
		return actionEtcdCache;
	}

	@Override
	public ActionEtcdCache getActionResultCache() {
		return actionResultEtcdCache;
	}

	@Override
	public PriorityQueueEtcdCache getPriorityQueueCache() {
		return priorityQueueEtcdCache;
	}

	@Override
	public LockAttributionEtcdCache getLockAttributionCache() {
		return lockAttributionEtcdCache;
	}

	@Override
	public void deleteAllCaches() throws EtcdServiceException {
		for (AbstractEtcdCache<?> c : allCaches) {
			c.deleteCache();
		}
	}

}
