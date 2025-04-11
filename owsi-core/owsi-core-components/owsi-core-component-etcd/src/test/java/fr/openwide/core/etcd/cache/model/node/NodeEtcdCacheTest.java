package fr.openwide.core.etcd.cache.model.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class NodeEtcdCacheTest extends AbstractEtcdTest {

	@Test
	public void deleteCacheWithLeaseTest() throws Exception {

		long leaseTtl = 30;

		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).build();

		String cacheName = "deleteCacheWithLeaseTest";

		String key = "nodeName";

		try (Client client = buildEctdClient(etcdConfig)) {
			EtcdClientClusterConfiguration clientConfiguration = new EtcdClientClusterConfiguration(etcdConfig, client);
			NodeEtcdCache nodeCache = new NodeEtcdCache(cacheName, clientConfiguration);
			nodeCache.put(key, NodeEtcdValue.from(new Date(), cacheName));
			final NodeEtcdValue valueFromCache = nodeCache.get(key);
			assertThat(valueFromCache).isNotNull();

			nodeCache.delete(key);
			final NodeEtcdValue valueFromCacheAfterDelete = nodeCache.get(key);
			assertThat(valueFromCacheAfterDelete).isNull();
		}

	}

    @Test
	public void leaseExpirationCacheTest() throws Exception {
    	
		long leaseTtl = 1;

		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(leaseTtl).build();

		String cacheName = "leaseExpirationCacheTest";

		String key = "nodeName";

		try (Client client = buildEctdClient(etcdConfig)) {
			EtcdClientClusterConfiguration clientConfiguration = new EtcdClientClusterConfiguration(etcdConfig, client);
			NodeEtcdCache nodeCache = new NodeEtcdCache(cacheName, clientConfiguration);
			nodeCache.put(key, NodeEtcdValue.from(new Date(), cacheName));
			final NodeEtcdValue valueFromCache = nodeCache.get(key);
			assertThat(valueFromCache).isNotNull();
			clientConfiguration.getIsShutdown().set(true);
		}

		CompletableFuture<NodeEtcdValue> nodeEtcdValueFuture = CompletableFuture.supplyAsync(() -> {
			try {
				EtcdClientClusterConfiguration clientConfiguration = new EtcdClientClusterConfiguration(etcdConfig,
						buildEctdClient(etcdConfig));
				NodeEtcdCache anotherNodeCache = new NodeEtcdCache(cacheName, clientConfiguration);
				return anotherNodeCache.get(key);
			} catch (EtcdServiceException e) {
				throw new IllegalStateException(e);
			}
		}, // Wait lease expiration
				CompletableFuture.delayedExecutor(leaseTtl + 2, TimeUnit.SECONDS));

		assertThat(nodeEtcdValueFuture.join()).as("Key should be deleted with lease expiration").isNull();
    }

} 