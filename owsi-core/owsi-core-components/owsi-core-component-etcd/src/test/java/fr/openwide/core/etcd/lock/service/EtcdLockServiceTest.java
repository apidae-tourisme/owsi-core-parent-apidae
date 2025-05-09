package fr.openwide.core.etcd.lock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class EtcdLockServiceTest extends AbstractEtcdTest {

	private IEtcdLockService lockService1;
	private EtcdClientClusterConfiguration clientConfiguration1;
	private IEtcdLockService lockService2;
	private EtcdClientClusterConfiguration clientConfiguration2;

	private static final String LOCK_1 = "lock1";
	private static final String LOCK_2 = "lock2";
    
    @Before
    public void setUp() throws Exception {
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(30).withLockTimeout(1).withNodeName("node_EtcdLockServiceTest").build();

		clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		lockService1 = new EtcdLockService(clientConfiguration1);

		clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		lockService2 = new EtcdLockService(clientConfiguration2);
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
			boolean result = lockService1.tryLock(LOCK_1);
			assertThat(result).isTrue();
		} finally {
			lockService1.unlock(LOCK_1);
		}
    }
    
	@Test
	public void testTryLockFailure() throws Exception {
		assertThat(lockService1.tryLock(LOCK_1)).isTrue();
		assertThat(lockService2.tryLock(LOCK_1)).isFalse();
		assertThat(lockService2.tryLock(LOCK_2)).isTrue();
		assertThat(lockService1.tryLock(LOCK_2)).isFalse();
		assertThat(lockService1.tryLock(LOCK_2)).isFalse();
		lockService1.unlock(LOCK_1);
		assertThat(lockService2.tryLock(LOCK_1)).isTrue();
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
			assertThat(new EtcdLockService(clientConfiguration2).tryLock(LOCK_1)).isTrue();
			clientConfiguration2.getIsShutdown().set(true);
		}

		CompletableFuture<Boolean> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			EtcdLockService anotherLockService = new EtcdLockService(
					new EtcdClientClusterConfiguration(etcdConfig, buildEctdClient(etcdConfig)));
			return anotherLockService.tryLock(LOCK_1);
		}, // Wait lease expiration
				CompletableFuture.delayedExecutor(leaseTtl + 1, TimeUnit.SECONDS));

		assertThat(secondTaskFuture.join()).isTrue();
	}

} 