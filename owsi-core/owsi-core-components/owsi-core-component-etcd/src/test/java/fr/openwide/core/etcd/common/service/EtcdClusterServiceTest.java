package fr.openwide.core.etcd.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdValue;
import fr.openwide.core.etcd.cache.model.priorityqueue.RoleAttribution;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.infinispan.model.DoIfRoleWithLock;
import fr.openwide.core.infinispan.model.ILock;
import fr.openwide.core.infinispan.model.IPriorityQueue;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.infinispan.model.SimpleLock;
import fr.openwide.core.infinispan.model.SimpleRole;
import fr.openwide.core.infinispan.model.impl.LockRequest;
import fr.openwide.core.infinispan.model.impl.SimplePriorityQueue;

public class EtcdClusterServiceTest extends AbstractEtcdTest {

	IEtcdClusterService etcdClusterService1;

	IEtcdClusterService etcdClusterService2;

	private static final IRole ROLE_1 = SimpleRole.from("ROLE_NAME_1");

	private static final ILock LOCK_1 = SimpleLock.from("LOCK_NAME", "LOCK_TYPE");

	@Before
	public void setUp() throws Exception {
		final EtcdCommonClusterConfiguration etcdConfigNode1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(5).withLockTimeout(1).withNodeName("node_1").build();
		etcdClusterService1 = new EtcdClusterService(etcdConfigNode1);
		etcdClusterService1.init();

		final EtcdCommonClusterConfiguration etcdConfigNode2 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(5).withLockTimeout(1).withNodeName("node_2").build();
		etcdClusterService2 = new EtcdClusterService(etcdConfigNode2);
		etcdClusterService2.init();
	}

	@After
	@Override
	public void cleanUp() throws Exception {
		Optional.ofNullable(etcdClusterService1).ifPresent(c -> c.stop());
		Optional.ofNullable(etcdClusterService2).ifPresent(c -> c.stop());
		super.cleanUp();
	}

	@Test
	public void testDoIfRoleWithLock_Run() throws EtcdServiceException, ExecutionException {
		AtomicBoolean wasExecuted = new AtomicBoolean(false);

		Runnable task = () -> {
			wasExecuted.set(true);
		};

		etcdClusterService1.assignRole(ROLE_1);

		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1);

		final DoIfRoleWithLock doIfRoleWithLock = etcdClusterService1.doIfRoleWithLock(lockRequest, task);

		assertThat(doIfRoleWithLock).isEqualTo(DoIfRoleWithLock.RUN);
		assertThat(wasExecuted).isTrue();
	}

	@Test
	public void testDoIfRoleWithLock_RoleNotOwned() throws ExecutionException {
		AtomicBoolean wasExecuted = new AtomicBoolean(false);

		Runnable task = () -> {
			wasExecuted.set(true);
		};

		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1);

		final DoIfRoleWithLock doIfRoleWithLock = etcdClusterService1.doIfRoleWithLock(lockRequest, task);

		assertThat(doIfRoleWithLock).isEqualTo(DoIfRoleWithLock.NOT_RUN_ROLE_NOT_OWNED);
		assertThat(wasExecuted).isFalse();
	}

	@Test
	public void testDoIfRoleWithLock_NotRunLockNotAvailable() throws EtcdServiceException {
		AtomicBoolean wasLaunchTask1 = new AtomicBoolean(false);
		Semaphore semaphore = new Semaphore(0);
		AtomicBoolean wasExecutedTask1 = new AtomicBoolean(false);

		AtomicBoolean wasExecutedTask2 = new AtomicBoolean(false);

		Runnable task1 = () -> {
			wasLaunchTask1.set(true);
			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			wasExecutedTask1.set(true);
		};

		Runnable task2 = () -> {
			wasExecutedTask2.set(true);
		};

		etcdClusterService1.assignRole(ROLE_1);

		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1);

		CompletableFuture<DoIfRoleWithLock> mainTaskFuture = CompletableFuture.supplyAsync(() -> {
			try {
				DoIfRoleWithLock lock = etcdClusterService1.doIfRoleWithLock(lockRequest, task1);
				return lock;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});

		CompletableFuture<DoIfRoleWithLock> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return etcdClusterService1.doIfRoleWithLock(lockRequest, task2);
			} catch (ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}, // Ajout d'un délai pour s'assurer que la permière tâche est lancée
				CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));

		assertThat(secondTaskFuture.join()).isEqualTo(DoIfRoleWithLock.NOT_RUN_LOCK_NOT_AVAILABLE);
		assertThat(wasLaunchTask1).as("task 1 should be launched").isTrue();
		assertThat(wasExecutedTask2).as("task 2 should not have been executed").isFalse();

		// Unlock main task
		semaphore.release();
		assertThat(mainTaskFuture.join()).isEqualTo(DoIfRoleWithLock.RUN);
		assertThat(wasExecutedTask1).as("task 1 should be executed").isTrue();
	}

	@Test
	public void testAssignRole() throws EtcdServiceException {
		etcdClusterService1.assignRole(ROLE_1);
		etcdClusterService1.getRoles();
		assertThat(etcdClusterService1.getRoles()).containsKey(ROLE_1.getKey());
		etcdClusterService1.unassignRole(ROLE_1);
		assertThat(etcdClusterService1.getRoles()).doesNotContainKey(ROLE_1.getKey());
	}

	@Test
	public void initTest() throws Exception {
		String currentNodeName = "nodeUpdateTest";
		long leaseTTl = 2;

		final EtcdCommonClusterConfiguration etcdConfigNode1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTTl).withLockTimeout(1).withNodeName(currentNodeName).build();
		try (EtcdClusterService clusterService = new EtcdClusterService(etcdConfigNode1)) {
			clusterService.init();
			assertThat(newNodeEtcdCache().get(currentNodeName)).as("Node is inserted in node cache after service init")
					.isNotNull();

			CompletableFuture<NodeEtcdValue> nodeValueFromCacheFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return newNodeEtcdCache().get(currentNodeName);
				} catch (EtcdServiceException e) {
					throw new IllegalStateException(e);
				}
			}, // Vérification du renouvellement du lease
					CompletableFuture.delayedExecutor(leaseTTl + 1, TimeUnit.SECONDS));
			assertThat(nodeValueFromCacheFuture.join())
					.as("Node is still present from cache because service is still alive").isNotNull();
		}

		CompletableFuture<NodeEtcdValue> nodeValueFromCacheAfterExpirationFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return newNodeEtcdCache().get(currentNodeName);
			} catch (EtcdServiceException e) {
				throw new IllegalStateException(e);
			}
		}, // Ajout d'un délai pour attendre l'expiration du lease.
				CompletableFuture.delayedExecutor(leaseTTl + 1, TimeUnit.SECONDS));
		assertThat(nodeValueFromCacheAfterExpirationFuture.join()).as("Node is deleted from cache after service stop")
				.isNull();
	}

	@Test
	public void testDoWithLockPriority_Success() throws Exception {
		AtomicBoolean wasExecuted = new AtomicBoolean(false);

		Runnable task = () -> {
			wasExecuted.set(true);
		};
		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1,
				SimplePriorityQueue.from("testDoWithLockPriority_Success"));

		boolean result = etcdClusterService1.doWithLockPriority(lockRequest, task);

		assertThat(result).isTrue();
		assertThat(wasExecuted).isTrue();
	}

	@Test
	public void testDoWithLockPriority_WithoutLock() throws Exception {
		AtomicBoolean wasExecuted = new AtomicBoolean(false);

		Runnable task = () -> {
			wasExecuted.set(true);
		};

		// Create a lock request without a lock, only with role
		LockRequest lockRequest = LockRequest.with(ROLE_1, null, SimplePriorityQueue.from("TEST_PRIORITY_QUEUE"));

		boolean result = etcdClusterService1.doWithLockPriority(lockRequest, task);

		assertThat(result).isTrue();
		assertThat(wasExecuted).isTrue();
	}

	@Test
	public void testDoWithLockPriority_LockNotAvailable() throws Exception {
		AtomicBoolean wasExecutedTask1 = new AtomicBoolean(false);
		AtomicBoolean wasExecutedTask2 = new AtomicBoolean(false);
		Semaphore semaphore = new Semaphore(0);

		Runnable task1 = () -> {
			try {
				wasExecutedTask1.set(true);
				semaphore.acquire();
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		};

		Runnable task2 = () -> {
			wasExecutedTask2.set(true);
		};

		// Créer une PriorityQueue pour le test
		IPriorityQueue priorityQueue = SimplePriorityQueue.from("testDoWithLockPriority_LockNotAvailable");
		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1, priorityQueue);

		// Préparer la file de priorité avec le service1 en premier
		List<RoleAttribution> attributions = new ArrayList<>();
		attributions.add(RoleAttribution.from(etcdClusterService1.getNodeName(), new Date()));
		attributions.add(RoleAttribution.from(etcdClusterService2.getNodeName(), new Date()));
		
		// Ajouter la file de priorité manuellement
		PriorityQueueEtcdValue queueValue = PriorityQueueEtcdValue.from(new Date(), "test", "UPDATE", attributions);
		etcdClusterService1.getCacheManager().getPriorityQueueCache().put(priorityQueue.getKey(), queueValue);

		CompletableFuture<Boolean> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return etcdClusterService2.doWithLockPriority(lockRequest, task2);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});

		CompletableFuture<Boolean> mainTaskFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return etcdClusterService1.doWithLockPriority(lockRequest, task1);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		
		// La seconde tâche ne doit pas être exécutée car le verrou est déjà pris
		assertThat(secondTaskFuture.join()).isFalse();
		assertThat(wasExecutedTask2).as("task 2 should not be executed").isFalse();

		// Libérer la première tâche
		semaphore.release();
		assertThat(mainTaskFuture.join()).isTrue();
		assertThat(wasExecutedTask1).as("task 1 should be executed").isTrue();
	}

	@Test
	public void testDoWithLockPriority_ClusterInactive() throws Exception {
		// Simuler un cluster inactif
		IEtcdClusterService etcdClusterServiceSpy = Mockito.spy(etcdClusterService1);
		Mockito.when(etcdClusterServiceSpy.isClusterActive()).thenReturn(false);

		AtomicBoolean wasExecuted = new AtomicBoolean(false);

		Runnable task = () -> {
			wasExecuted.set(true);
		};

		// Ajouter un IPriorityQueue au LockRequest
		IPriorityQueue priorityQueue = SimplePriorityQueue.from("testDoWithLockPriority_ClusterInactive");
		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1, priorityQueue);

		boolean result = etcdClusterServiceSpy.doWithLockPriority(lockRequest, task);

		assertThat(result).isFalse();
		assertThat(wasExecuted).isFalse();
	}

	@Test
	public void testDoWithLockPriority_PriorityQueuePositioning() throws Exception {
		AtomicBoolean wasExecutedTask1 = new AtomicBoolean(false);
		AtomicBoolean wasExecutedTask2 = new AtomicBoolean(false);
		
		Runnable task1 = () -> {
			wasExecutedTask1.set(true);
		};
		
		Runnable task2 = () -> {
			wasExecutedTask2.set(true);
		};
		
		// Créer une SimplePriorityQueue
		IPriorityQueue priorityQueue = SimplePriorityQueue.from("TEST_PRIORITY_QUEUE");

		// Créer un LockRequest avec la priorityQueue
		LockRequest lockRequest = LockRequest.with(ROLE_1, LOCK_1, priorityQueue);
		
		// Vérifier que la priorité est respectée
		// On ajoute manuellement une entrée dans la file de priorité pour etcdClusterService2
		List<RoleAttribution> attributions = new ArrayList<>();
		attributions.add(RoleAttribution.from(etcdClusterService2.getNodeName(), new Date()));
		attributions.add(RoleAttribution.from(etcdClusterService1.getNodeName(), new Date()));
		
		// Ajouter la file de priorité manuellement
		PriorityQueueEtcdValue queueValue = PriorityQueueEtcdValue.from(new Date(), "test", null, attributions);
		etcdClusterService1.getCacheManager().getPriorityQueueCache().put(priorityQueue.getKey(), queueValue);
		
		// etcdClusterService2 devrait avoir la priorité
		CompletableFuture<Boolean> service1Future = CompletableFuture.supplyAsync(() -> {
			try {
				return etcdClusterService1.doWithLockPriority(lockRequest, task1);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		
		CompletableFuture<Boolean> service2Future = CompletableFuture.supplyAsync(() -> {
			try {
				return etcdClusterService2.doWithLockPriority(lockRequest, task2);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		
		assertThat(service2Future.join()).isTrue();
		assertThat(wasExecutedTask2).isTrue();
		
		assertThat(service1Future.join()).isFalse();
		assertThat(wasExecutedTask1).isFalse();
	}

}
