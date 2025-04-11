package fr.openwide.core.etcd.coordinator.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class EtcdCoordinatorServiceTest extends AbstractEtcdTest {
    
	@Test
	public void updateCoordinatorTest() throws EtcdServiceException {
		Long leaseTtl = 2L;
		String node1 = "node_1_coordinatorAssociationTest";
		String node2 = "node_2_coordinatorAssociationTest";
		String node3 = "node_3_coordinatorAssociationTest";

		final EtcdCommonClusterConfiguration etcdConfig1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node1).withUpdateCoordinatorEnable(true)
				.build();
		final EtcdCommonClusterConfiguration etcdConfig2 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node2).withUpdateCoordinatorEnable(true)
				.build();
		final EtcdCommonClusterConfiguration etcdConfig3 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node3).withUpdateCoordinatorEnable(true)
				.build();

		IEtcdCoordinatorService coordinatorService1 = null;
		IEtcdCoordinatorService coordinatorService2 = null;

		Client client2 = buildEctdClient(etcdConfig2);

		try (Client client1 = buildEctdClient(etcdConfig1)) {
			EtcdClientClusterConfiguration clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig1,
					client1);
			coordinatorService1 = new EtcdCoordinatorService(clientConfiguration1);
			coordinatorService1.start();

			assertThat(coordinatorService1.getCurrentCoordinator()).isEqualTo(node1);
			assertThat(coordinatorService1.isCoordinator()).as("coordinator node is node_1").isTrue();

			EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig2,
					client2);
			coordinatorService2 = new EtcdCoordinatorService(clientConfiguration2);
			coordinatorService2.start();
			assertThat(coordinatorService1.isCoordinator()).as("coordinator node is still node_1").isTrue();
			assertThat(coordinatorService2.isCoordinator()).as("coordinator node is still node_1").isFalse();
		}

		CompletableFuture<String> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			EtcdClientClusterConfiguration clientConfig = new EtcdClientClusterConfiguration(etcdConfig3,
					buildEctdClient(etcdConfig3));
			try {
				return new EtcdCoordinatorService(clientConfig).getCurrentCoordinator();
			} catch (EtcdServiceException e) {
				throw new IllegalStateException(e);
			}
		}, // Wait lease expiration
				CompletableFuture.delayedExecutor(leaseTtl + 1, TimeUnit.SECONDS));
		assertThat(secondTaskFuture.join()).isEqualTo(node2);
		coordinatorService2.stop();
	}

	@Test
	public void stopTest() throws EtcdServiceException {
		String node1 = "node_1_stopTest";
		IEtcdCoordinatorService coordinatorService1 = newCoordinatorService(node1);
		coordinatorService1.start();
		assertThat(coordinatorService1.getCurrentCoordinator()).isEqualTo(node1);
		coordinatorService1.stop();
		assertThat(coordinatorService1.getCurrentCoordinator()).isNull();
	}

	@Test
	public void deleteCoordinatorKeyIfOwnedTest() throws EtcdServiceException {
		String node1 = "node_1_stopTest";
		String node2 = "node_2_stopTest";

		IEtcdCoordinatorService coordinatorService1 = newCoordinatorService(node1);
		coordinatorService1.start();
		assertThat(coordinatorService1.getCurrentCoordinator()).isEqualTo(node1);

		IEtcdCoordinatorService coordinatorService2 = newCoordinatorService(node2);
		assertThat(coordinatorService1.getCurrentCoordinator()).as("coordinator is still node 1").isEqualTo(node1);
		assertThat(coordinatorService2.deleteCoordinatorKeyIfOwned()).isFalse();
		assertThat(coordinatorService1.deleteCoordinatorKeyIfOwned()).isTrue();
		assertThat(coordinatorService1.getCurrentCoordinator()).isNull();

	}

	private IEtcdCoordinatorService newCoordinatorService(String node) {
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLockTimeout(1).withNodeName(node).build();
		EtcdClientClusterConfiguration clientConfiguration = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		return new EtcdCoordinatorService(clientConfiguration);
	}

	@Test
	public void deleteCoordinatorTest() throws EtcdServiceException {
		String node1 = "node_1_deleteCoordinatorTest";

		final EtcdCommonClusterConfiguration etcdConfig1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLockTimeout(1).withNodeName(node1).build();

		EtcdClientClusterConfiguration clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig1,
				buildEctdClient(etcdConfig1));
		IEtcdCoordinatorService coordinatorService1 = new EtcdCoordinatorService(clientConfiguration1);
		coordinatorService1.start();
		assertThat(coordinatorService1.getCurrentCoordinator()).isEqualTo(clientConfiguration1.getNodeName());
		coordinatorService1.deleteCoordinator();
		assertThat(coordinatorService1.getCurrentCoordinator()).isNull();
	}

} 