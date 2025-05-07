package fr.openwide.core.etcd.common.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.action.model.RoleRebalanceEtcdAction;
import fr.openwide.core.etcd.action.service.EtcdActionService;
import fr.openwide.core.etcd.action.service.IEtcdActionService;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdValue;
import fr.openwide.core.etcd.cache.model.priorityqueue.RoleAttribution;
import fr.openwide.core.etcd.cache.model.role.RoleEtcdValue;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdValue;
import fr.openwide.core.etcd.cache.service.EtcdCacheManager;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.exception.EtcdServiceRuntimeException;
import fr.openwide.core.etcd.common.model.DoIfRoleWithLock;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.etcd.coordinator.service.EtcdCoordinatorService;
import fr.openwide.core.etcd.coordinator.service.IEtcdCoordinatorService;
import fr.openwide.core.etcd.lock.service.EtcdLockService;
import fr.openwide.core.infinispan.action.SwitchRoleResult;
import fr.openwide.core.infinispan.model.ILock;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.infinispan.service.IRolesProvider;
import fr.openwide.core.infinispan.service.InfinispanClusterServiceImpl;
import io.etcd.jetcd.Client;

public class EtcdClusterService implements IEtcdClusterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClusterService.class);

	private final EtcdCacheManager etcdCacheManager;
	private final EtcdLockService lockService;
	private final EtcdCommonClusterConfiguration config;
	private final EtcdClientClusterConfiguration clientConfiguration;

	private final Client etcdClient;

	private final ScheduledThreadPoolExecutor rebalanceExecutor = new ScheduledThreadPoolExecutor(1,
			new ThreadFactoryBuilder().setNameFormat("etcd-%d").build());

	private final ScheduledThreadPoolExecutor checkerExecutor = new ScheduledThreadPoolExecutor(1,
			new ThreadFactoryBuilder().setNameFormat("etcd-checker-%d").build());

	private final IEtcdActionService actionService;
	private final IEtcdCoordinatorService coordinatorService;

	public EtcdClusterService(EtcdCommonClusterConfiguration config) {
		this.config = config;
		this.etcdClient = Client.builder().endpoints(config.getEndpoints()).waitForReady(true)
				.connectTimeout(Duration.ofSeconds(config.getConnectTimeout())).build();
		this.clientConfiguration = new EtcdClientClusterConfiguration(config, etcdClient);
		this.etcdCacheManager = new EtcdCacheManager(clientConfiguration);
		this.lockService = new EtcdLockService(clientConfiguration);
		this.actionService = new EtcdActionService(this, clientConfiguration, config.getActionFactory());
		this.coordinatorService = new EtcdCoordinatorService(clientConfiguration);

		initExecutor(this.rebalanceExecutor);
		initExecutor(this.checkerExecutor);
	}

	private void initExecutor(ScheduledThreadPoolExecutor executor) {
		executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		executor.prestartAllCoreThreads();
	}

	@Override
	public synchronized void init() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting ETCD cluster service for node {}", getNodeName());
		}
		clientConfiguration.getIsShutdown().set(false);
		try {
			etcdCacheManager.init();
		} catch (EtcdServiceException e) {
			throw new IllegalStateException("Error starting cache manager", e);
		}
		coordinatorService.start();

		actionService.start();

		try {
			LOGGER.debug("Register node informations {}", getNodeName());
			NodeEtcdValue node = NodeEtcdValue.from(new Date(), getNodeName());
			etcdCacheManager.getNodeCache().put(getNodeName(), node);
		} catch (EtcdServiceException e) {
			throw new IllegalStateException("Error adding node %s in cache".formatted(getNodeName()), e);
		}

		rebalanceExecutor.schedule(this::rebalanceRoles, 1, TimeUnit.MINUTES);
		checkerExecutor.scheduleAtFixedRate(this::updateCoordinator, 1, 5, TimeUnit.MINUTES);
	}

	@Override
	public synchronized void stop() {
		LOGGER.debug("Stopping ETCD for node {}", getNodeName());
		if (!clientConfiguration.getIsShutdown().compareAndSet(false, true)) {
			LOGGER.warn("Stop seems be called twice on {}", getNodeName());
		}
		actionService.stop();

		try {
			LOGGER.debug("Clean role owned by current node {}", getNodeName());
			etcdCacheManager.getRoleCache().cleanCacheFromNode(getNodeName());
		} catch (EtcdServiceException e) {
			LOGGER.error("Error cleaning role owned by current node {}", getNodeName(), e);
		}

		try {
			etcdCacheManager.stop();
		} catch (EtcdServiceException e) {
			throw new IllegalStateException("Error stopping cache manager", e);
		}

		coordinatorService.stop();

		Optional.ofNullable(etcdClient).ifPresent(Client::close);

		stopExecutor(checkerExecutor, "checkerExecutor");
		stopExecutor(rebalanceExecutor, "executor");
	}

	@Override
	public boolean doWithLockPriority(ILockRequest lockRequest, Runnable runnable) throws ExecutionException {
		if (!isClusterActive()) {
			return false;
		}

		// First check if we already have a slot in the priority queue
		boolean prioritySlotFound = false;
		List<RoleAttribution> values;
		try {
			PriorityQueueEtcdValue queueValue = etcdCacheManager.getPriorityQueueCache()
					.get(lockRequest.getPriorityQueue().getKey());
			if (queueValue != null) {
				values = queueValue.getAttributions();
				for (RoleAttribution attribution : values) {
					if (attribution.match(getNodeName())) {
						prioritySlotFound = true;
						break;
					}
				}
			} else {
				values = new ArrayList<>();
			}
		} catch (EtcdServiceException e) {
			throw new ExecutionException("Failed to check priority queue", e);
		}

		// If no slot found, add one
		if (!prioritySlotFound) {
			try {
				values.add(RoleAttribution.from(getNodeName(), new Date()));
				PriorityQueueEtcdValue newValue = PriorityQueueEtcdValue.from(new Date(), getNodeName(), values);
				etcdCacheManager.getPriorityQueueCache().put(lockRequest.getPriorityQueue().getKey(), newValue);
			} catch (EtcdServiceException e) {
				throw new ExecutionException("Failed to add to priority queue", e);
			}
		}

		boolean priorityAllowed = false;
		// Check if we're first in line
		if (!values.isEmpty() && values.get(0).match(getNodeName())) {
			priorityAllowed = true;
		}

		if (priorityAllowed) {
			try {
				// Try to acquire the lock and run the task
				if (lockRequest.getLock() != null) {
					boolean lockAcquired = lockService.tryLock(lockRequest.getLock().getKey());
					if (lockAcquired) {
						try {
							runnable.run();
							return true;
						} finally {
							lockService.unlock(lockRequest.getLock().getKey());
						}
					}
				} else {
					runnable.run();
					return true;
				}
			} finally {
				// Remove our slot from the priority queue
				try {
					List<RoleAttribution> updatedValues = new ArrayList<>();
					for (RoleAttribution attribution : values) {
						if (!attribution.match(getNodeName())) {
							updatedValues.add(attribution);
						}
					}
					PriorityQueueEtcdValue updatedValue = PriorityQueueEtcdValue.from(new Date(), getNodeName(),
							updatedValues);
					etcdCacheManager.getPriorityQueueCache().put(lockRequest.getPriorityQueue().getKey(), updatedValue);
				} catch (EtcdServiceException e) {
					LOGGER.error("Failed to remove from priority queue", e);
				}
			}
		}

		return false;
	}

	@Override
	public DoIfRoleWithLock doIfRoleWithLock(ILockRequest lockRequest, Runnable runnable) throws EtcdServiceException {
		if (!isClusterActive()) {
			return DoIfRoleWithLock.NOT_RUN_CLUSTER_UNAVAILABLE;
		}

		RoleEtcdValue roleValue = etcdCacheManager.getRoleCache().get(lockRequest.getRole().getKey());
		if (roleValue == null || !Objects.equal(roleValue.getNodeName(), getNodeName())) {
			return DoIfRoleWithLock.NOT_RUN_ROLE_NOT_OWNED;
		}

		// If no lock is required, just run the task
		if (lockRequest.getLock() == null) {
			runnable.run();
			return DoIfRoleWithLock.RUN;
		}

		// Try to acquire the lock using etcd's distributed lock
		String lockName = lockRequest.getLock().getKey();
		boolean lockAcquired = lockService.tryLock(lockName);

		if (!lockAcquired) {
			return DoIfRoleWithLock.NOT_RUN_LOCK_NOT_AVAILABLE;
		}

		try {
			// Run the task
			runnable.run();
			return DoIfRoleWithLock.RUN;
		} finally {
			// Always release the lock
			lockService.unlock(lockName);
		}
	}

	@Override
	public <T> T syncedAction(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
			throws ExecutionException, TimeoutException {
		try {
			return actionService.syncedAction(action, timeout, unit);
		} catch (EtcdServiceException e) {
			throw new EtcdServiceRuntimeException(e);
		}
	}

	@Override
	public void assignRole(IRole role) throws EtcdServiceException {
		tryAssignRole(role, new ArrayList<>(), new ArrayList<>());
	}

	private void tryAssignRole(IRole role, List<IRole> acquiredRoles, List<IRole> newRoles)
			throws EtcdServiceException {
		if (role != null && role.getKey() != null) {
			RoleEtcdValue previousRoleEtcdValue = getCacheManager().getRoleCache().putIfAbsent(role.getKey(),
					RoleEtcdValue.from(new Date(), getNodeName()));
			if (previousRoleEtcdValue != null) {
				if (!Objects.equal(previousRoleEtcdValue.getNodeName(), getNodeName())) {
					LOGGER.warn("Role rebalance on {} fails; already attributed to {} {}", role, previousRoleEtcdValue,
							getNodeName());
				} else {
					LOGGER.warn("Role rebalance on {} uselessly; already attributed {}", role, getNodeName());
					acquiredRoles.add(role);
				}
			} else {
				LOGGER.info("Role rebalance - request on {} succeeded {}", role, getNodeName());
				acquiredRoles.add(role);
				newRoles.add(role);
			}
		}

	}

	@Override
	public void unassignRole(IRole role) throws EtcdServiceException {
		if (role != null && role.getKey() != null) {
			etcdCacheManager.getRoleCache().delete(role.getKey());
		}
	}

	@Override
	public Map<String, RoleEtcdValue> getRoles() throws EtcdServiceException {
		return etcdCacheManager.getRoleCache().getAll().entrySet().stream()
				.filter(map -> map.getValue() != null && Objects.equal(map.getValue().getNodeName(), getNodeName()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public EtcdLockService getLockService() {
		return lockService;
	}

	private void rebalanceRoles() {
		if (config.isRoleRebalanceEnable()) {
			try {
				actionService.resultLessAction(RoleRebalanceEtcdAction.rebalance(getNodeName()));
			} catch (EtcdServiceException e) {
				LOGGER.error("Error trying to trigger rebalance roles action", e);
			}
		}
	}

	@Override
	public void doRebalanceRoles() {
		try {
			doRebalanceRoles(1000);
		} catch (EtcdServiceException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean isClusterActive() {
		return coordinatorService.isClusterActive();
	}

	private void doRebalanceRoles(int waitWeight) throws EtcdServiceException {
		LOGGER.debug("Starting role rebalance {}", getNodeName());
		if (!isClusterActive()) {
			LOGGER.error("Cluster is marked as inactive (split detected); ignore rebalance");
			return;
		}

		List<IRole> roles = Lists.newArrayList(config.getRoleProvider().getRebalanceRoles());
		List<IRole> acquiredRoles = Lists.newArrayList();
		List<IRole> newRoles = Lists.newArrayList();
		while (!roles.isEmpty() && !Thread.currentThread().isInterrupted()) {
			try {
				// (aquiredRoles number * waitWeight * rand(1->2)) ms
				// the most roles we acquire, the more we wait (to let other
				// nodes a chance to acquire new roles)
				TimeUnit.MILLISECONDS.sleep((waitWeight * acquiredRoles.size() * (1 + Math.round(Math.random()))));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.warn("Interrupted while rebalancing {}", getNodeName());
				return;
			}
			IRole role = roles.remove(0);
			RoleRequestEtcdValue request = rebalanceRole(role, acquiredRoles, newRoles);

			// get rid of request if is acquired
			if (acquiredRoles.contains(role) && request != null && Objects.equal(request.getNodeName(), getNodeName())
					&& getCacheManager().getRoleRequestCache().deleteIfNodeMatches(role.getKey(),
							request.getNodeName())) {
				LOGGER.info("Role rebalance - honored request {} removed {}", request, getNodeName());
			}
		}
		LOGGER.debug("Ending role rebalance {}", getNodeName());
		if (!newRoles.isEmpty() && LOGGER.isInfoEnabled()) {
			LOGGER.info("Role rebalance - new roles acquired: {} {}", Joiner.on(",").join(newRoles), getNodeName());
			LOGGER.debug("Role rebalance - roles: {} {}", Joiner.on(",").join(acquiredRoles), getNodeName());
		}
	}

	private RoleRequestEtcdValue rebalanceRole(IRole role, List<IRole> acquiredRoles, List<IRole> newRoles)
			throws EtcdServiceException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting role rebalance - trying {} for {}", role, getNodeName());
		}
		RoleRequestEtcdValue request = getCacheManager().getRoleRequestCache().get(role.getKey());
		if (request != null) {
			// check already existing request
			if (Objects.equal(request.getNodeName(), getNodeName())) {
				tryAssignRole(role, acquiredRoles, newRoles);
			} else {
				LOGGER.warn("Role rebalance - {} skipped because it exists a running request on it {}", role,
						getNodeName());
			}
		} else {
			// no request, acquire it if not attributed
			tryAssignRole(role, acquiredRoles, newRoles);
		}
		return request;
	}

	private void updateCoordinator() {
		if (config.isUpdateCoordinatorEnable()) {
			coordinatorService.tryBecomeCoordinator();
		}
	}

	/**
	 * @return whether the local node is the cluster's coordinator
	 */
	public boolean isCoordinator() {
		return coordinatorService.isCoordinator();
	}

	private void stopExecutor(ScheduledThreadPoolExecutor executor, String executorName) {
		// stop accepting new tasks
		executor.shutdown();
		try {
			executor.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			List<Runnable> runnables = executor.shutdownNow();
			if (!runnables.isEmpty()) {
				LOGGER.warn("{} tasks dropped by {}.{}", runnables.size(),
						InfinispanClusterServiceImpl.class.getSimpleName(), executorName);
			}
		}
	}

	@Override
	public String getNodeName() {
		return clientConfiguration.getNodeName();
	}

	private IRolesProvider getRoleProvider() {
		return clientConfiguration.getClusterConfiguration().getRoleProvider();
	}

	@Override
	public Set<IRole> getAllRolesForAssignation() {
		return Set.copyOf(getRoleProvider().getRoles());
	}

	@Override
	public Set<IRole> getAllRolesForRolesRequests() {
		return Set.copyOf(getRoleProvider().getRoles());
	}

	@Override
	public EtcdCacheManager getCacheManager() {
		return etcdCacheManager;
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Override
	public IEtcdCoordinatorService getCoordinatorService() {
		return coordinatorService;
	}

	@Override
	public Map<String, NodeEtcdValue> getNodes() {
		try {
			return getCacheManager().getNodeCache().getAll();
		} catch (EtcdServiceException e) {
			throw new EtcdServiceRuntimeException(e);
		}
	}

	@Override
	public RoleEtcdValue getRole(IRole roleKey) {
		if (roleKey == null) {
			return null;
		}
		try {
			return getCacheManager().getRoleCache().get(roleKey.getKey());
		} catch (EtcdServiceException e) {
			throw new EtcdServiceRuntimeException(e);
		}
	}

	@Override
	public Pair<SwitchRoleResult, String> assignRole(IRole iRole, NodeEtcdValue nodeValue) {
		// FIXME
		return null;
	}

	@Override
	public Set<ILock> getLocks() {
		// FIXME
		return null;
	}

}
