package fr.openwide.core.infinispan.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.BoundType;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import fr.openwide.core.commons.util.functional.SerializableFunction;
import fr.openwide.core.infinispan.action.RebalanceAction;
import fr.openwide.core.infinispan.action.RoleCaptureAction;
import fr.openwide.core.infinispan.action.RoleReleaseAction;
import fr.openwide.core.infinispan.action.SwitchRoleResult;
import fr.openwide.core.infinispan.listener.CacheEntryCreateEventListener;
import fr.openwide.core.infinispan.listener.ViewChangedEventCoordinatorListener;
import fr.openwide.core.infinispan.model.DoIfRoleWithLock;
import fr.openwide.core.infinispan.model.IAction;
import fr.openwide.core.infinispan.model.IAttribution;
import fr.openwide.core.infinispan.model.ILeaveEvent;
import fr.openwide.core.infinispan.model.ILock;
import fr.openwide.core.infinispan.model.ILockAttribution;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.INode;
import fr.openwide.core.infinispan.model.IPriorityQueue;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.infinispan.model.IRoleAttribution;
import fr.openwide.core.infinispan.model.impl.Attribution;
import fr.openwide.core.infinispan.model.impl.LeaveEvent;
import fr.openwide.core.infinispan.model.impl.LockAttribution;
import fr.openwide.core.infinispan.model.impl.Node;
import fr.openwide.core.infinispan.model.impl.RoleAttribution;

public class InfinispanClusterServiceImpl implements IInfinispanClusterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanClusterServiceImpl.class);

	private static final String CACHE_ACTIONS = "__ACTIONS__";
	private static final String CACHE_ACTIONS_RESULTS = "__ACTIONS_RESULTS__";
	private static final String CACHE_LEAVE = "__LEAVE__";
	private static final String CACHE_LOCKS = "__LOCKS__";
	private static final String CACHE_NODES = "__NODES__";
	private static final String CACHE_PRIORITY_QUEUES = "__PRIORITY_QUEUES__";
	private static final String CACHE_ROLES = "__ROLES__";
	private static final String CACHE_ROLES_REQUESTS = "__ROLES_REQUESTS__";

	private static final List<String> CACHES;
	static {
		Builder<String> builder = ImmutableList.builder();
		builder.add(CACHE_ACTIONS, CACHE_LEAVE, CACHE_LOCKS, CACHE_NODES, CACHE_PRIORITY_QUEUES, CACHE_ROLES,
				CACHE_ROLES_REQUESTS);
		CACHES = builder.build();
	}

	private final String nodeName;
	private final EmbeddedCacheManager cacheManager;
	private final IRolesProvider rolesProvider;
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
			new ThreadFactoryBuilder().setNameFormat("infinispan-%d").build());
	private final ScheduledThreadPoolExecutor checkerExecutor = new ScheduledThreadPoolExecutor(1,
			new ThreadFactoryBuilder().setNameFormat("infinispan-checker-%d").build());
	private final IActionFactory actionFactory;
	private final IInfinispanClusterCheckerService infinispanClusterCheckerService;

	private final ConcurrentMap<String, Object> actionMonitors = Maps.<String, Object> newConcurrentMap();

	private boolean initialized = false;
	private boolean stopped = false;

	public InfinispanClusterServiceImpl(String nodeName, EmbeddedCacheManager cacheManager,
			IRolesProvider rolesProvider, IActionFactory actionFactory,
			IInfinispanClusterCheckerService infinispanClusterCheckerService) {
		super();
		this.nodeName = nodeName;
		this.cacheManager = cacheManager;
		this.rolesProvider = rolesProvider;
		this.actionFactory = actionFactory;
		// don't wait for delayed tasks after shutdown (even if already planned)
		initExecutor(this.executor);
		this.infinispanClusterCheckerService = infinispanClusterCheckerService;
		if (infinispanClusterCheckerService != null) {
			initExecutor(this.checkerExecutor);
		}
	}

	private void initExecutor(ScheduledThreadPoolExecutor executor) {
		executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		executor.prestartAllCoreThreads();
	}

	@Override
	public synchronized void init() {
		LOGGER.debug("Starting cacheManager");
		cacheManager.start();
		LOGGER.debug("cacheManager started");
		String address = String.format("[%s]", getAddress());
		if (!initialized) {
			LOGGER.debug("{} Initializing {}", address, toStringClusterNode());

			LOGGER.debug("{} Starting caches", address);
			for (String cacheName : CACHES) {
				if (!cacheManager.isRunning(cacheName)) {
					cacheManager.getCache(cacheName, true);
					if (address == null) {
						address = String.format("[%s]", getAddress());
					}
					LOGGER.debug("{} Cache {} started", address, cacheName);
				} else {
					LOGGER.debug("{} Cache {} already running", address, cacheName);
				}
			}
			LOGGER.debug("{} Caches started", address);

			LOGGER.debug("{} Viewed members {}", address, Joiner.on(",").join(cacheManager.getMembers()));
			Node node = Node.from(getAddress(), nodeName);
			LOGGER.debug("{} Register node informations {}", address, node);
			getNodesCache().put(getAddress(), node);

			LOGGER.debug("{} Register listeners", address);
			// view change
			cacheManager.addListener(new ViewChangedEventCoordinatorListener(this));

			// action queue
			getActionsCache().addListener(new CacheEntryCreateEventListener<IAction<?>>() {
				@Override
				public void onAction(CacheEntryEvent<String, IAction<?>> value) {
					InfinispanClusterServiceImpl.this.onAction(value);
				}
			});

			// result queue
			getActionsResultsCache().addListener(new CacheEntryCreateEventListener<IAction<?>>() {
				@Override
				public void onAction(CacheEntryEvent<String, IAction<?>> value) {
					InfinispanClusterServiceImpl.this.onResult(value);
				}
			});

			if (cacheManager.isCoordinator() && infinispanClusterCheckerService != null) {
				infinispanClusterCheckerService.updateCoordinator(getLocalAddress(), Collections.emptyList());
			}

			executor.schedule(new Runnable() {
				@Override
				public void run() {
					InfinispanClusterServiceImpl.this.rebalanceRoles();
				}
			}, 1, TimeUnit.MINUTES);
			checkerExecutor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					InfinispanClusterServiceImpl.this.updateCoordinator();
				}
			}, 1, 5, TimeUnit.MINUTES);

			initialized = true;
		}
	}

	@Override
	public EmbeddedCacheManager getCacheManager() {
		return cacheManager;
	}

	@Override
	public List<Address> getMembers() {
		return ImmutableList
				.<Address> copyOf(Lists.transform(cacheManager.getMembers(), INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS));
	}

	@Override
	public List<INode> getNodes() {
		return ImmutableList.copyOf(Iterables.filter(Iterables.transform(getMembers(), new Function<Address, INode>() {
			@Override
			public INode apply(Address input) {
				return getNodesCache().get(input);
			}
		}), Predicates.notNull()));
	}

	@Override
	public List<INode> getAllNodes() {
		return ImmutableList.copyOf(getNodesCache().values());
	}

	@Override
	public Set<ILock> getLocks() {
		return ImmutableSet.copyOf(getLocksCache().keySet());
	}

	@Override
	public ILockAttribution getLockAttribution(ILock iLock) {
		return getLocksCache().get(iLock);
	}

	@Override
	public Set<IRole> getAllRolesForAssignation() {
		return ImmutableSet.<IRole> builder().addAll(rolesProvider.getRoles()).addAll(getRolesCache().keySet()).build();
	}

	@Override
	public Set<IRole> getAllRolesForRolesRequests() {
		return ImmutableSet.<IRole> builder().addAll(rolesProvider.getRoles()).addAll(getRolesRequestsCache().keySet())
				.build();
	}

	@Override
	public IRoleAttribution getRoleAttribution(IRole iRole) {
		return getRolesCache().get(iRole);
	}

	@Override
	public IAttribution getRoleRequestAttribution(IRole iRole) {
		return getRolesRequestsCache().get(iRole);
	}

	@Override
	public synchronized void stop() {
		String address = String.format("[%s]", getAddress());
		if (initialized) {
			LOGGER.warn("Stopping {}", InfinispanClusterServiceImpl.class.getSimpleName());
			if (stopped) {
				LOGGER.warn("{} Stop seems be called twice on {}", address, toStringClusterNode());
			}

			Date leaveDate = new Date();

			INode previousNode = getNodesCache().get(getAddress());
			Node node = Node.from(previousNode, leaveDate);

			// signal normal leave event
			getLeaveCache().put(getAddress(), LeaveEvent.from(leaveDate));
			// update to last known status
			getNodesCache().put(getAddress(), node);

			for (Entry<IRole, IRoleAttribution> roleEntry : getRolesCache().entrySet()) {
				if (roleEntry.getValue() != null && roleEntry.getValue().match(getAddress())) {
					// race condition; we can remove an third-party assignation
					getRolesCache().remove(roleEntry.getKey());
				}
			}
			for (Entry<IRole, IAttribution> role : getRolesRequestsCache().entrySet()) {
				if (role.getValue() != null && role.getValue().match(getAddress())) {
					// race condition; we can remove an third-party assignation
					getRolesRequestsCache().remove(role.getKey());
				}
			}

			// priority queue · clean asked priority
			for (Entry<IPriorityQueue, List<IAttribution>> priorityQueueEntry : getPriorityQueuesCache().entrySet()) {
				boolean commitPriorityQueue = true;
				try {
					// lock entry
					getPriorityQueuesCache().startBatch();
					getPriorityQueuesCache().getAdvancedCache().lock(priorityQueueEntry.getKey());

					// build new cleaned new value
					List<IAttribution> updatedPriorityQueue = Lists.newArrayList();
					for (IAttribution attribution : priorityQueueEntry.getValue()) {
						if (!attribution.match(getAddress())) {
							updatedPriorityQueue.add(attribution);
						}
					}

					// put
					getPriorityQueuesCache().put(priorityQueueEntry.getKey(), updatedPriorityQueue);
				} finally {
					getPriorityQueuesCache().endBatch(commitPriorityQueue);
				}
			}

			// actions: kept for debugging
			// actions result: kept for debugging
			// locks: kept for history (finally block must remove remaining
			// locks)

			{
				// unset in cluster checker
				stopExecutor(checkerExecutor, "checkerExecutor");
				if (cacheManager.isCoordinator() && infinispanClusterCheckerService != null) {
					infinispanClusterCheckerService.unsetCoordinator(getLocalAddress());
				}
			}
			
			cacheManager.stop();
			
			stopExecutor(executor, "executor");
			
			stopped = true;
			LOGGER.warn("Stopped {}", InfinispanClusterServiceImpl.class.getSimpleName());
		} else {
			LOGGER.warn("{} Ignored stop event as cluster not initialized {}", address, toStringClusterNode());
		}
	}

	private void stopExecutor(ScheduledThreadPoolExecutor executor, String executorName) {
		// stop accepting new tasks
		executor.shutdown();
		try {
			executor.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		} // NOSONAR
		List<Runnable> runnables = executor.shutdownNow();
		if (!runnables.isEmpty()) {
			LOGGER.warn("{} tasks dropped by {}.{}", runnables.size(),
					InfinispanClusterServiceImpl.class.getSimpleName(), executorName);
		}
	}

	private String toStringClusterNode() {
		return String.format("%s:%s:%s", getClass().getSimpleName(), cacheManager.getClusterName(), getAddress());
	}

	@Override
	public String getClusterIdentifier() {
		List<Address> members = getMembers();
		Collections.sort(members);
		return Joiner.on(",").join(members);
	}

	@Override
	public boolean isClusterActive() {
		if (infinispanClusterCheckerService == null) {
			// no consistency check
			return true;
		} else {
			return infinispanClusterCheckerService.isClusterActive(
					Collections2.transform(cacheManager.getMembers(), INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS));
		}
	}

	@Override
	public DoIfRoleWithLock doIfRoleWithLock(ILockRequest lockRequest, Runnable runnable) throws ExecutionException {
		if (!isClusterActive()) {
			return DoIfRoleWithLock.NOT_RUN_CLUSTER_UNAVAILABLE;
		}
		// try to retrieve lock
		IRoleAttribution roleAttribution = getRolesCache().getOrDefault(lockRequest.getRole(), null);
		try {
			if (roleAttribution != null && roleAttribution.match(getAddress())) {
				// if lock is owned, we can run
				if (lockRequest.getLock() != null) {
					return doWithLock(lockRequest.getLock(), runnable) ?
							DoIfRoleWithLock.RUN : DoIfRoleWithLock.NOT_RUN_LOCK_NOT_AVAILABLE;
				} else {
					runnable.run();
					return DoIfRoleWithLock.RUN;
				}
			} else {
				// else return false
				return DoIfRoleWithLock.NOT_RUN_ROLE_NOT_OWNED;
			}
		} catch (RuntimeException e) {
			throw new ExecutionException(e);
		}
	}

	@Override
	public boolean doWithLock(ILock withLock, Runnable runnable) throws ExecutionException {
		if (!isClusterActive()) {
			return false;
		}
		// try to retrieve lock
		ILockAttribution previousLockAttribution = getLocksCache().putIfAbsent(withLock,
				LockAttribution.from(getAddress(), new Date()));
		try {
			if (previousLockAttribution == null) {
				// if lock was absent we can run
				runnable.run();
				return true;
			} else {
				//if myself
				if (previousLockAttribution.getOwner().equals(getAddress())) {
					getLocksCache().remove(withLock);
					return doWithLock(withLock, runnable);
				}
				// else return false
				return false;
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new ExecutionException(e);
		} finally {
			// if lock was attributed, we need to clear status
			if (previousLockAttribution == null) {
				ILockAttribution removedAttribution = getLocksCache().remove(withLock);
				if (removedAttribution == null || !removedAttribution.match(getAddress())) {
					// as we put value only if absent, we must not retrieve another
					// value than `Address` here
					LOGGER.warn("Inconsistent address removal: expected `{}` ; removed `{}`", getAddress(),
							removedAttribution != null ? removedAttribution.getOwner() : "null");
				}
			}
		}
	}

	@Override
	public boolean doWithLockPriority(ILockRequest lockRequest, Runnable runnable) throws ExecutionException {
		if (!isClusterActive()) {
			return false;
		}
		Cache<IPriorityQueue, List<IAttribution>> cache = getPriorityQueuesCache();
		boolean commit = true;
		boolean priorityAllowed = true;
		
		if (!cache.startBatch()) {
			LOGGER.error("Batch attempt on {} failed", lockRequest.getPriorityQueue());
		}
		try {
			// get lock values
			if (!cache.getAdvancedCache().lock(lockRequest.getPriorityQueue())) {
				LOGGER.error("Lock attempt on {} failed", lockRequest.getPriorityQueue());
				commit = false;
				return false;
			}
			List<IAttribution> values = cache.getOrDefault(lockRequest.getPriorityQueue(),
					Lists.<IAttribution> newArrayList());
			// check if a slot is already kept
			boolean prioritySlotFound = false;
			for (IAttribution attribution : values) {
				if (attribution.match(getAddress())) {
					prioritySlotFound = true;
				}
			}
			
			// if no slot, add it
			if (!prioritySlotFound) {
				// get a priority slot if absent
				values.add(Attribution.from(getAddress(), new Date()));
				cache.put(lockRequest.getPriorityQueue(), values);
			}
			
			// if slot is first, we can do our job
			if (values.size() > 0 && values.get(0).match(getAddress())) {
				// priority is allowed (first slot taken)
				// allow second phase and remove priority slot
				priorityAllowed = true;
			} else {
				// priority is not allowed, retry later when it is our slot turn
				priorityAllowed = false;
			}
		} finally {
			cache.endBatch(commit);
		}

		if (priorityAllowed) {
			// return doWithLock result (true - job done ; false - job not launch)
			try {
				return doWithLock(lockRequest.getLock(), runnable);
			} finally {
				// get rid of this node slot
				filterPriorityQueue(lockRequest.getPriorityQueue(),
						new Predicate<IAttribution>() {
							@Override
							public boolean apply(IAttribution input) {
								// keep all attribution of other nodes
								return !input.match(getAddress());
							}
					
				});
			}
		} else {
			return false;
		}
	}

	private void filterPriorityQueue(IPriorityQueue priorityQueue, Predicate<IAttribution> attributionPredicate) {
		Cache<IPriorityQueue, List<IAttribution>> cache = getPriorityQueuesCache();
		boolean commit = false;
		if (!cache.getAdvancedCache().startBatch()) {
			LOGGER.error("Batch attempt on {} failed", priorityQueue);
		}
		try {
			// get lock values
			if (!cache.getAdvancedCache().lock(priorityQueue)) {
				LOGGER.error("Lock attempt on {} failed", priorityQueue);
				commit = false;
				return;
			}
			List<IAttribution> values = cache.getOrDefault(priorityQueue, Lists.<IAttribution> newArrayList());
			List<IAttribution> newValues = Lists.newArrayList(Collections2.filter(values, attributionPredicate));
			cache.put(priorityQueue, newValues);
			commit = true;
		} finally {
			cache.endBatch(commit);
		}
	}

	@Override
	public Address getLocalAddress() {
		return INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS.apply(cacheManager.getAddress());
	}

	private Address getAddress() {
		if (cacheManager.getAddress() != null) {
			return INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS.apply(cacheManager.getAddress());
		} else {
			return null;
		}
	}

	private Cache<IRole, IRoleAttribution> getRolesCache() {
		return cacheManager.<IRole, IRoleAttribution> getCache(CACHE_ROLES);
	}

	private Cache<IPriorityQueue, List<IAttribution>> getPriorityQueuesCache() {
		return cacheManager.<IPriorityQueue, List<IAttribution>> getCache(CACHE_PRIORITY_QUEUES);
	}

	private Cache<Address, ILeaveEvent> getLeaveCache() {
		return cacheManager.<Address, ILeaveEvent> getCache(CACHE_LEAVE);
	}

	private Cache<IRole, IAttribution> getRolesRequestsCache() {
		return cacheManager.<IRole, IAttribution> getCache(CACHE_ROLES_REQUESTS);
	}

	private Cache<ILock, ILockAttribution> getLocksCache() {
		return cacheManager.<ILock, ILockAttribution> getCache(CACHE_LOCKS);
	}

	private Cache<Address, INode> getNodesCache() {
		return cacheManager.<Address, INode> getCache(CACHE_NODES);
	}

	private Cache<String, IAction<?>> getActionsCache() {
		return cacheManager.<String, IAction<?>> getCache(CACHE_ACTIONS);
	}

	private Cache<String, IAction<?>> getActionsResultsCache() {
		return cacheManager.<String, IAction<?>> getCache(CACHE_ACTIONS_RESULTS);
	}

	@Override
	public void rebalanceRoles(boolean clearRoles, Collection<String> rolesToKeep) {
		if (clearRoles) {
			Collection<IRole> roles = Lists.newArrayList(rolesProvider.getRoles());
			Collection<IRole> rolesToRemove = Lists.newArrayList();
			if (rolesToKeep != null) {
				for (IRole role : roles) {
					if (rolesToKeep.contains(role.getKey())) {
						rolesToRemove.add(role);
					}
				}
			}
			
			roles.removeAll(rolesToRemove);
			
			for (IRole role : roles) {
				getRolesCache().remove(role);
			}
		}
		
		rebalanceRoles();
	}

	public void rebalanceRoles() {
		resultLessAction(RebalanceAction.rebalance(getAddress()));
	}

	@Override
	public void onViewChangedEvent(ViewChangedEvent viewChangedEvent) {
		// NOTE : lists cannot be null
		List<Address> newMembers = Lists.transform(viewChangedEvent.getNewMembers(),
				INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS);
		List<Address> oldMembers = Lists.transform(viewChangedEvent.getOldMembers(),
				INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS);

		List<Address> added = Lists.newArrayList(newMembers);
		added.removeAll(oldMembers);
		List<Address> removed = Lists.newArrayList(oldMembers);
		removed.removeAll(newMembers);

		// all known nodes, either currently in cluster or that leave gracefully
		List<Address> knownNodes = Lists.newArrayList();

		LOGGER.debug("Processing view removed nodes ({}) {}", removed.size(), toStringClusterNode());
		for (Address removedItem : removed) {
			LOGGER.debug("Processing view removed node {}", removedItem, toStringClusterNode());
			ILeaveEvent leaveEvent = getLeaveCache().getOrDefault(removedItem, null);
			INode node = getNodesCache().getOrDefault(removedItem, null);
			if (leaveEvent == null) {
				if (node == null) {
					LOGGER.warn("Unknown node {} left cluster without leave event {}", removedItem,
							toStringClusterNode());
				} else {
					LOGGER.warn("Node {} left cluster without leave event {}", node, toStringClusterNode());
				}
			} else {
				Date now = new Date();
				long elapsed = now.getTime() - leaveEvent.getLeaveDate().getTime();
				String timing = String.format("%d ms.", elapsed);
				if (node == null) {
					LOGGER.warn("Unknown node {} left cluster with leave event {} (elapsed {})", removedItem,
							toStringClusterNode(), timing);
				} else {
					knownNodes.add(node.getAddress());
					LOGGER.info("Node {} left cluster without leave event {} (elapsed {})", node, toStringClusterNode(),
							timing);
				}
			}
		}

		LOGGER.debug("Processing view added items ({}) {}", added.size(), toStringClusterNode());
		for (Address addedItem : added) {
			knownNodes.add(addedItem);
			if (viewChangedEvent.isMergeView()) {
				LOGGER.warn("Merge node {} {}", addedItem, toStringClusterNode());
			} else {
				LOGGER.debug("New node {} {}", addedItem, toStringClusterNode());
			}
		}

		if (infinispanClusterCheckerService != null) {
			boolean status = infinispanClusterCheckerService.updateCoordinator(getLocalAddress(), knownNodes);
			if (!status) {
				LOGGER.error("Cluster JDBC status update failed on view change !");
			}
		}

		// if view change is due to a split, roles kept assigned
		// rebalance should be safe as only unassigned roles may be rebalanced
		rebalanceRoles();
	}

	@Override
	public void unassignRole(IRole iRole) {
		getRolesCache().remove(iRole);
	}

	@Override
	public void removeRoleRequest(IRole iRole) {
		getRolesRequestsCache().remove(iRole);
	}

	@Override
	public Pair<SwitchRoleResult, String> assignRole(IRole iRole, INode iNode) {
		// push request, ask for release, ask for capture, return
		Stopwatch switchWatch = Stopwatch.createStarted();

		// request (replace old element is allowed)
		getRolesRequestsCache().put(iRole, RoleAttribution.from(iNode.getAddress(), new Date()));
		IRoleAttribution roleAttribution = getRolesCache().get(iRole);

		Pair<SwitchRoleResult, String> stepResult = null;

		// release
		if (roleAttribution != null) {
			Stopwatch releaseWatch = Stopwatch.createStarted();

			try {
				stepResult = syncedAction(RoleReleaseAction.release(roleAttribution.getOwner(), iRole), 1,
						TimeUnit.SECONDS);
			} catch (ExecutionException e) {
				return Pair.with(SwitchRoleResult.SWITCH_UNKNOWN_ERROR,
						String.format("Unknown exception during release - %s", e.getCause().getMessage()));
			} catch (TimeoutException e) {
				return Pair.with(SwitchRoleResult.SWITCH_RELEASE_TIMEOUT, String.format(
						"Timeout during release (%d ms. elapsed)", releaseWatch.elapsed(TimeUnit.MILLISECONDS)));
			}

			if (!SwitchRoleResult.SWITCH_STEP_SUCCESS.equals(stepResult.getValue0())) {
				return stepResult;
			}
		}

		// capture
		{
			Stopwatch captureWatch = Stopwatch.createStarted();
			try {
				stepResult = syncedAction(RoleCaptureAction.capture(iNode.getAddress(), iRole), 10, TimeUnit.SECONDS);
			} catch (ExecutionException e) {
				return Pair.with(SwitchRoleResult.SWITCH_UNKNOWN_ERROR,
						String.format("Unknown exception during capture - %s", e.getCause().getMessage()));
			} catch (TimeoutException e) {
				return Pair.with(SwitchRoleResult.SWITCH_CAPTURE_TIMEOUT, String.format(
						"Timeout during release (%d ms. elapsed)", captureWatch.elapsed(TimeUnit.MILLISECONDS)));
			}
		}

		// return
		if (!SwitchRoleResult.SWITCH_STEP_SUCCESS.equals(stepResult.getValue0())) {
			return stepResult;
		} else {
			return Pair.with(SwitchRoleResult.SWITCH_SUCCESS,
					String.format("Switch done in %d ms.", switchWatch.elapsed(TimeUnit.MILLISECONDS)));
		}
	}

	@Override
	public void doRebalanceRoles() {
		doRebalanceRoles(1000);
	}

	@Override
	public Pair<SwitchRoleResult, String> doReleaseRole(IRole role) {
		if (!getRolesCache().getAdvancedCache().startBatch()) {
			throw new IllegalStateException("Nested batch detected!");
		}
		boolean commit = true;
		boolean releaseDone = false;
		getRolesCache().getAdvancedCache().lock(role);
		try {
			IRoleAttribution attribution = getRolesCache().get(role);
			if (attribution != null && attribution.match(getAddress())) {
				getRolesCache().remove(role);
				releaseDone = true;
			}
		} finally {
			getRolesCache().getAdvancedCache().endBatch(commit);
		}

		if (getRolesCache().get(role) == null) {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_SUCCESS, "OK");
		} else if (releaseDone) {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_INCONSISTENCY, "Map remove done, but role not released !");
		} else {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_INCONSISTENCY, "Map remove cannot be done !");
		}
	}

	@Override
	public Pair<SwitchRoleResult, String> doCaptureRole(IRole role) {
		// role request checked in a insecure way (but role capture is safe)
		if (getRolesRequestsCache().get(role) != null && !getRolesRequestsCache().get(role).match(getAddress())) {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_INCONSISTENCY, "Role is already requested by another node");
		}

		getRolesCache().putIfAbsent(role, RoleAttribution.from(getAddress(), new Date()));

		// request release not safe as it can conflict with another capture
		getRolesRequestsCache().remove(role);

		if (getRolesCache().get(role) == null) {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_INCONSISTENCY, "Role is available but was not captured");
		} else if (!getRolesCache().get(role).match(getAddress())) {
			return Pair.with(SwitchRoleResult.SWITCH_CAPTURE_NOT_AVAILABLE, "Role is not available at capture time");
		} else {
			return Pair.with(SwitchRoleResult.SWITCH_STEP_SUCCESS, "OK");
		}
	}

	public void doRebalanceRoles(int waitWeight) {
		LOGGER.debug("Starting role rebalance {}", toStringClusterNode());
		if (!isClusterActive()) {
			LOGGER.error("Cluster is marked as inactive (split detected); ignore rebalance");
			return;
		}

		List<IRole> roles = Lists.newArrayList(rolesProvider.getRebalanceRoles());
		List<IRole> acquiredRoles = Lists.newArrayList();
		List<IRole> newRoles = Lists.newArrayList();
		while (roles.size() > 0 && !Thread.currentThread().isInterrupted()) {
			try {
				// (aquiredRoles number * waitWeight * rand(1->2)) ms
				// the most roles we acquire, the more we wait (to let other
				// nodes a chance to acquire new roles)
				TimeUnit.MILLISECONDS.sleep((waitWeight * acquiredRoles.size() * (1 + Math.round(Math.random()))));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.warn("Interrupted while rebalancing {}", toStringClusterNode());
				return;
			}
			IRole role = roles.remove(0);
			LOGGER.debug("Starting role rebalance - trying {} {}", role, toStringClusterNode());
			IAttribution request = getRolesRequestsCache().getOrDefault(role, null);
			if (request != null) {
				// check already existing request
				if (request.match(getAddress())) {
					IAttribution previousAttribution = getRolesCache().putIfAbsent(role,
							RoleAttribution.from(getAddress(), new Date()));
					if (previousAttribution != null) {
						if (!previousAttribution.match(getAddress())) {
							LOGGER.warn("Role rebalance - request on {} fails; already attributed to {} {}", role,
									previousAttribution, toStringClusterNode());
						} else {
							LOGGER.warn("Role rebalance - request on {} uselessly; already attributed {}", role,
									toStringClusterNode());
							acquiredRoles.add(role);
						}
					} else {
						LOGGER.info("Role rebalance - request on {} succeeded {}", role, toStringClusterNode());
						acquiredRoles.add(role);
						newRoles.add(role);
					}
				} else {
					// TODO timeout request (?)
					LOGGER.warn("Role rebalance - {} skipped because it exists a running request on it {}", role,
							toStringClusterNode());
				}
			} else {
				// no request, acquire it if not attributed
				IAttribution previousAttribution = getRolesCache().putIfAbsent(role,
						RoleAttribution.from(getAddress(), new Date()));
				if (previousAttribution != null) {
					if (!previousAttribution.match(getAddress())) {
						LOGGER.debug("Role rebalance - try {} uselessly; already attributed to {} {}", role,
								previousAttribution, toStringClusterNode());
					} else {
						LOGGER.debug("Role rebalance - try {} uselessly; already attributed {}", role,
								toStringClusterNode());
						acquiredRoles.add(role);
					}
				} else {
					LOGGER.info("Role rebalance - try on {} succeeded {}", role, toStringClusterNode());
					acquiredRoles.add(role);
					newRoles.add(role);
				}
			}

			// get rid of request if is acquired
			if (acquiredRoles.contains(role) && request != null && request.match(getAddress())) {
				if (getRolesRequestsCache().remove(role, request)) {
					LOGGER.info("Role rebalance - honored request {} removed {}", request, toStringClusterNode());
				}
			}
		}

		LOGGER.debug("Ending role rebalance {}", toStringClusterNode());

		if (!newRoles.isEmpty()) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Role rebalance - new roles acquired: {} {}", Joiner.on(",").join(newRoles),
						toStringClusterNode());
				LOGGER.debug("Role rebalance - roles: {} {}", Joiner.on(",").join(acquiredRoles),
						toStringClusterNode());
			}
		}
	}

	protected void onAction(CacheEntryEvent<String, IAction<?>> value) {
		final String key = value.getKey();
		final IAction<?> action = value.getValue();
		if (value.getValue().isBroadcast() || getAddress().equals(value.getValue().getTarget())) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						LOGGER.debug("action {} on {}", action.getClass().getSimpleName(), getLocalAddress());
						action.setInfinispanClusterService(InfinispanClusterServiceImpl.this);
						if (actionFactory != null) {
							actionFactory.prepareAction(action);
						}
						action.doRun();
						if (action.needsResult()) {
							getActionsResultsCache().put(value.getKey(), action);
						}
					} catch (RuntimeException e) {
						LOGGER.error("Error during action {}", action.getClass().getSimpleName(), e);
					} finally {
						getActionsCache().remove(key);
					}
				}
			});
		}
	}

	protected void onResult(CacheEntryEvent<String, IAction<?>> value) {
		if (actionMonitors.containsKey(value.getKey())) {
			Object monitor = actionMonitors.get(value.getKey());
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
		actionMonitors.remove(value.getKey());
	}

	private <A extends IAction<?>> String resultLessAction(A action) {
		String uniqueID = UUID.randomUUID().toString();
		getActionsCache().put(uniqueID, action);
		return uniqueID;
	}

	@Override
	public <A extends IAction<V>, V> V syncedAction(A action, int timeout, TimeUnit unit)
			throws ExecutionException, TimeoutException {
		String uniqueID = UUID.randomUUID().toString();
		
		// actionMonitors allow to optimize wakeup when we wait for a result.
		Object monitor = new Object();
		actionMonitors.putIfAbsent(uniqueID, monitor);
		
		getActionsCache().put(uniqueID, action);
		
		if (action.needsResult()) {
			Stopwatch stopwatch = Stopwatch.createUnstarted();
			while (timeout == -1 || stopwatch.elapsed(TimeUnit.MILLISECONDS) < unit.toMillis(timeout)) {
				if (!stopwatch.isRunning()) {
					stopwatch.start();
				}
				@SuppressWarnings("unchecked")
				A result = (A) getActionsResultsCache().remove(uniqueID);
				if (result != null && result.isDone()) {
					try {
						return result.get();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} else if (result != null && result.isCancelled()) {
					throw new CancellationException();
				}
				synchronized (monitor) {
					try {
						unit.timedWait(monitor, timeout);
					} catch (InterruptedException e) {
					} // NOSONAR
				}
			}
		} else {
			return null;
		}
		
		throw new TimeoutException();
	}

	/**
	 * TODO: drop; needed when we use both jgroups address and infinispan address.
	 */
	private static final class ToJgroupsAddress
			implements SerializableFunction<Address, Address> {
		private static final long serialVersionUID = -6249484113042442830L;

		@Override
		public Address apply(org.infinispan.remoting.transport.Address input) {
			return input;
		}
	}

	private void updateCoordinator() {
		try {
			if (infinispanClusterCheckerService != null && cacheManager.isCoordinator()) {
				boolean updated = infinispanClusterCheckerService.updateCoordinatorTimestamp(getLocalAddress());
				if (!updated) {
					LOGGER.warn("Infinispan checker coordinator update failed by {}", getLocalAddress());
					updated = infinispanClusterCheckerService.tryForceUpdate(getLocalAddress(), 6, TimeUnit.MINUTES);
					if (updated) {
						LOGGER.warn("Infinispan checker coordinator update forced by {}", getLocalAddress());
					}
				}
				
				if (updated) {
					cleanCachesByCoordinator();
					rebalanceRoles();
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Unknown error updating infinispan checker coordinator", e);
		}
	}

	private void cleanCachesByCoordinator() {
		if (isClusterActive()) {
			// FT - race condition are not handled in a clean way.
			{
				List<Entry<IRole, IRoleAttribution>> roleEntries = Lists.newArrayList(getRolesCache().entrySet());
				for (Entry<IRole, IRoleAttribution> roleEntry : roleEntries) {
					if (! getMembers().contains(roleEntry.getValue().getOwner())) {
						IRoleAttribution roleAttribution = getRolesCache().remove(roleEntry.getKey());
						LOGGER.warn("Remove {} as {} is absent", roleEntry.getKey(), roleEntry.getValue());
						// check if removed attribution is the right one
						if (! roleAttribution.match(roleEntry.getValue().getOwner())) {
							// if not, put back removed element
							getRolesCache().put(roleEntry.getKey(),
									RoleAttribution.from(roleAttribution.getOwner(), new Date()));
							LOGGER.warn("Reput {} as removed is linked to {}", roleEntry.getKey(), roleAttribution);
						}
					}
				}
			}
	
			{
				List<Entry<IRole, IAttribution>> roleEntries = Lists.newArrayList(getRolesRequestsCache().entrySet());
				for (Entry<IRole, IAttribution> roleEntry : roleEntries) {
					if (! getMembers().contains(roleEntry.getValue().getOwner())) {
						getRolesRequestsCache().remove(roleEntry.getKey());
						LOGGER.warn("Remove request on {} as {} is absent", roleEntry.getKey(), roleEntry.getValue());
						// no check as role request is not critical
					}
				}
			}
	
			{
				List<Entry<ILock, ILockAttribution>> lockEntries = Lists.newArrayList(getLocksCache().entrySet());
				for (Entry<ILock, ILockAttribution> lockEntry : lockEntries) {
					if (! getMembers().contains(lockEntry.getValue().getOwner())) {
						ILockAttribution lockAttribution = getLocksCache().remove(lockEntry.getKey());
						LOGGER.warn("Remove {} as {} is absent", lockEntry.getKey(), lockEntry.getValue());
						// check if removed attribution is the right one
						if (! lockAttribution.match(lockEntry.getValue().getOwner())) {
							// if not, put back removed element
							getLocksCache().put(lockEntry.getKey(),
									LockAttribution.from(lockAttribution.getOwner(), new Date()));
							LOGGER.warn("Reput {} as removed is linked to {}", lockEntry.getKey(), lockAttribution);
						}
					}
				}
			}
	
			{
				List<Entry<IPriorityQueue, List<IAttribution>>> priorityQueueEntries = Lists.newArrayList(getPriorityQueuesCache().entrySet());
				for (Entry<IPriorityQueue, List<IAttribution>> priorityQueueEntry : priorityQueueEntries) {
					// lazily check if items need to be removed
					boolean foundToDelete = false;
					for (IAttribution attribution : priorityQueueEntry.getValue()) {
						if (! getMembers().contains(attribution.getOwner())) {
							foundToDelete = true;
							break;
						}
					}
					
					if (foundToDelete) {
						LOGGER.warn("Cleaning priority queue for {}", priorityQueueEntry.getKey());
						// found orphan items, we lock and securely remove orphans
						filterPriorityQueue(priorityQueueEntry.getKey(), new Predicate<IAttribution>() {
							@Override
							public boolean apply(IAttribution input) {
								return getMembers().contains(input.getOwner());
							}
						});
					}
				}
			}
		}
	}

	@Override
	public Pair<Boolean, List<String>> checkRoles(boolean checkFairness) {
		boolean fair = true;
		List<String> comments = Lists.newArrayList();
		
		List<IRole> allRoles = Lists.newArrayList(rolesProvider.getRoles());
		allRoles.removeAll(getRolesCache().keySet());
		if ( ! allRoles.isEmpty()) {
			String status = String.format("Role balance has missing roles (missing roles: %s)",
					Joiner.on(",").join(allRoles));
			fair = false;
			comments.add(String.format(status));
		}
		
		if (checkFairness) {
			float rolesPerNode = (float) rolesProvider.getRoles().size() / cacheManager.getMembers().size();
			int allowedStep = 2;
			// we can't go lower than 0
			int lowerRolesPerNode = Math.max(0, Math.round(rolesPerNode - allowedStep));
			// we can't go higher than the number of roles
			int upperRolesPerNode = Math.min(rolesProvider.getRoles().size(), Math.round(rolesPerNode + allowedStep));
			
			Range<Integer> range = Range.range(lowerRolesPerNode, BoundType.CLOSED, upperRolesPerNode, BoundType.CLOSED);
			
			Map<IRole, IRoleAttribution> roles = Maps.newHashMap(getRolesCache());
			ListMultimap<Address, IRole> rolesByMember = roles.entrySet().stream().collect(
					Multimaps.<Entry<IRole, IRoleAttribution>, Address, IRole, ListMultimap<Address, IRole>>toMultimap(
							(item) -> item.getValue().getOwner(),
							(item) -> item.getKey(),
							MultimapBuilder.linkedHashKeys().arrayListValues()::<Address, IRole>build
			));
			for (Entry<Address, Collection<IRole>> rolesByMemberEntry : rolesByMember.asMap().entrySet()) {
				if ( ! range.contains(rolesByMemberEntry.getValue().size())) {
					String status = String.format("Role balance not fair on %s; %s not in %s (roles: %s)",
							rolesByMemberEntry.getKey(), rolesByMemberEntry.getValue().size(), range,
							Joiner.on(",").join(rolesByMemberEntry.getValue()));
					comments.add(status);
					LOGGER.warn(status);
					fair = false;
				}
			}
		}
		
		return Pair.with(fair, comments);
	}

	/**
	 * TODO: drop; needed when we use both jgroups address and infinispan address.
	 */
	private static final ToJgroupsAddress INFINISPAN_ADDRESS_TO_JGROUPS_ADDRESS = new ToJgroupsAddress();

}
