package fr.openwide.core.etcd.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.model.DoIfRoleWithLock;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.infinispan.model.ILock;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.infinispan.model.SimpleLock;
import fr.openwide.core.infinispan.model.SimpleRole;
import fr.openwide.core.infinispan.model.impl.LockRequest;

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
	public void testDoIfRoleWithLock_Run() throws EtcdServiceException {
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
	public void testDoIfRoleWithLock_RoleNotOwned() throws EtcdServiceException {
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
			} catch (EtcdServiceException e) {
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

}
