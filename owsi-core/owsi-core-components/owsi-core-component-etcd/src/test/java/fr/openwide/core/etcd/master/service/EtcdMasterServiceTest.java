package fr.openwide.core.etcd.master.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class EtcdMasterServiceTest extends AbstractEtcdTest {

    
	@Test
	public void masterAssociationTest() throws EtcdServiceException {
		Long leaseTtl = 2L;
		String node1 = "node_1";
		String node2 = "node_2";
		String node3 = "node_3";

		final EtcdCommonClusterConfiguration etcdConfig1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node1).build();
		final EtcdCommonClusterConfiguration etcdConfig2 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node2).build();
		final EtcdCommonClusterConfiguration etcdConfig3 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).withLockTimeout(1).withNodeName(node3).build();

		IEtcdMasterService masterService1 = null;
		IEtcdMasterService masterService2 = null;

		Client client2 = buildEctdClient(etcdConfig2);

		try (Client client1 = buildEctdClient(etcdConfig1)) {
			EtcdClientClusterConfiguration clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig1,
					client1);
			masterService1 = new EtcdMasterService(clientConfiguration1);
			masterService1.start();
			assertThat(masterService1.isMaster()).as("master node is node_1").isTrue();

			EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig2,
					client2);
			masterService2 = new EtcdMasterService(clientConfiguration2);
			masterService2.start();
			assertThat(masterService1.isMaster()).as("master node is still node_1").isTrue();
			assertThat(masterService2.isMaster()).as("master node is still node_1").isFalse();
		}

		CompletableFuture<String> secondTaskFuture = CompletableFuture.supplyAsync(() -> {
			EtcdClientClusterConfiguration clientConfig = new EtcdClientClusterConfiguration(etcdConfig3,
					buildEctdClient(etcdConfig3));
			try {
				return new EtcdMasterService(clientConfig).getCurrentMaster();
			} catch (EtcdServiceException e) {
				throw new IllegalStateException(e);
			}
		}, // Wait lease expiration
				CompletableFuture.delayedExecutor(leaseTtl + 1, TimeUnit.SECONDS));
		assertThat(secondTaskFuture.join()).isEqualTo(node2);
	}

} 