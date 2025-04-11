package fr.openwide.core.etcd.lock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.etcd.lock.model.EtcdLock;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.SimpleLock;
import fr.openwide.core.infinispan.model.SimpleRole;
import fr.openwide.core.infinispan.model.impl.LockRequest;
import io.etcd.jetcd.Client;

public class EtcdLockServiceTest extends AbstractEtcdTest {

	private IEtcdLockService lockService1;
	private EtcdClientClusterConfiguration clientConfiguration1;
	private IEtcdLockService lockService2;
	private EtcdClientClusterConfiguration clientConfiguration2;
	private LockAttributionEtcdCache lockAttributionEtcdCache;

	private static final String LOCK_1 = "lock1";
	private static final String LOCK_2 = "lock2";

	private static final ILockRequest LOCK_REQUEST_1 = LockRequest.with(SimpleRole.from("ROLE"),
			SimpleLock.from(LOCK_1, null));
	private static final ILockRequest LOCK_REQUEST_2 = LockRequest.with(SimpleRole.from("ROLE"),
			SimpleLock.from(LOCK_2, null));
    
    @Before
    public void setUp() throws Exception {
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(30).withLockTimeout(1).withNodeName("node_EtcdLockServiceTest").build();

		lockAttributionEtcdCache = new LockAttributionEtcdCache("atributionLockCache",
				new EtcdClientClusterConfiguration(etcdConfig, buildEctdClient(etcdConfig)));

		clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		lockService1 = new EtcdLockService(clientConfiguration1,
				lockAttributionEtcdCache);

		clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		lockService2 = new EtcdLockService(clientConfiguration2,
				lockAttributionEtcdCache);

    }

	@After
	@Override
	public void cleanUp() throws Exception {
		Optional.ofNullable(clientConfiguration1).ifPresent(c -> c.getIsShutdown().set(true));
		Optional.ofNullable(clientConfiguration2).ifPresent(c -> c.getIsShutdown().set(true));
		super.cleanUp();
	}

    @Test
    public void testTryLockSuccess() throws EtcdServiceException {
		try {
			boolean result = lockService1.tryLock(LOCK_REQUEST_1);
			assertThat(result).isTrue();
			assertThat(lockAttributionEtcdCache.getAllKeys()).containsExactlyInAnyOrder(LOCK_1);

		} finally {
			lockService1.unlock(LOCK_1);
			assertThat(lockAttributionEtcdCache.getAllKeys()).isEmpty();
		}
    }
    
	@Test
	public void testTryLockFailure() throws Exception {
		assertThat(lockService1.tryLock(LOCK_REQUEST_1)).isTrue();
		assertThat(lockService2.tryLock(LOCK_REQUEST_1)).isFalse();
		assertThat(lockService2.tryLock(LOCK_REQUEST_2)).isTrue();
		assertThat(lockService1.tryLock(LOCK_REQUEST_2)).isFalse();
		assertThat(lockService1.tryLock(LOCK_REQUEST_2)).isFalse();
		lockService1.unlock(LOCK_1);
		assertThat(lockService2.tryLock(LOCK_REQUEST_1)).isTrue();
	}

	@Test
	public void testLockLeaseExpired() throws EtcdServiceException {

		Long leaseTtl = 2L;

		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName("node_testLockleaseExpired").build();

		try (Client clientToClose = buildEctdClient(etcdConfig)) {
			EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
					clientToClose);
			// tryLock
			assertThat(new EtcdLockService(clientConfiguration2, lockAttributionEtcdCache).tryLock(LOCK_REQUEST_1))
					.isTrue();
			clientConfiguration2.getIsShutdown().set(true);
		}

		CompletableFuture<Boolean> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			EtcdLockService anotherLockService = new EtcdLockService(
					new EtcdClientClusterConfiguration(etcdConfig, buildEctdClient(etcdConfig)),
					lockAttributionEtcdCache);
			return anotherLockService.tryLock(LOCK_REQUEST_1);
		}, // Wait lease expiration
				CompletableFuture.delayedExecutor(leaseTtl + 1, TimeUnit.SECONDS));

		assertThat(secondTaskFuture.join()).isTrue();
	}

	@Test
	public void testGetAllLocks() throws EtcdServiceException {
		// Initially, there should be no locks
		Set<EtcdLock> initialLocks = lockService1.getAllLocks();
		assertThat(initialLocks).isEmpty();

		assertThat(lockAttributionEtcdCache.getAllKeys()).isEmpty();

		try {
			// Acquire some locks
			assertThat(lockService1.tryLock(LOCK_REQUEST_1)).isTrue();
			assertThat(lockService2.tryLock(LOCK_REQUEST_2)).isTrue();

			// Check that getAllLocks returns the expected locks
			Set<EtcdLock> allLocks = lockService1.getAllLocks();
			assertThat(allLocks).extracting("key").containsExactlyInAnyOrder(LOCK_1, LOCK_2);

			// Also verify from the second service
			Set<EtcdLock> allLocksFromService2 = lockService2.getAllLocks();
			assertThat(allLocksFromService2).extracting("key").containsExactlyInAnyOrder(LOCK_1, LOCK_2);

			assertThat(lockAttributionEtcdCache.getAllKeys()).containsExactlyInAnyOrder(LOCK_1, LOCK_2);

		} finally {
			// Clean up locks
			lockService1.unlock(LOCK_1);
			assertThat(lockAttributionEtcdCache.getAllKeys()).containsExactlyInAnyOrder(LOCK_2);
			lockService2.unlock(LOCK_2);
			assertThat(lockAttributionEtcdCache.getAllKeys()).isEmpty();
		}

		// After cleanup, there should be no locks again
		Set<EtcdLock> finalLocks = lockService1.getAllLocks();
		assertThat(finalLocks).isEmpty();
	}

} 