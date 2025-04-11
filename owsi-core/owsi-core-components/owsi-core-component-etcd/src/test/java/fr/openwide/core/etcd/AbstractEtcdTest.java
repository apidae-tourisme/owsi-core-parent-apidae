package fr.openwide.core.etcd;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdCache;
import fr.openwide.core.etcd.cache.service.EtcdCacheManager;
import fr.openwide.core.etcd.common.service.EtcdClusterService;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration.EtcdConfigurationBuilder;
import io.etcd.jetcd.Client;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EtcdConfig.class)
@TestPropertySource("classpath:etcd-test.properties")
public abstract class AbstractEtcdTest {

	@Value("${etcd.endpoints}")
	private String etcdEndpoints;

	protected String getEtcdEndpoints() {
		return etcdEndpoints;
	}

	protected static Client buildEctdClient(EtcdCommonClusterConfiguration etcdConfig) {
		return Client.builder().endpoints(etcdConfig.getEndpoints()).waitForReady(false).build();
	}

	protected EtcdConfigurationBuilder etcdConfigurationBuilderDefaultTestBuilder() {
		return EtcdCommonClusterConfiguration.builder().withEndpoints(getEtcdEndpoints())
				.withUpdateCoordinatorEnable(false).withRoleRebalanceEnable(false).withClusterName("SIT");
	}

	@After
	public void cleanUp() throws Exception {
		final EtcdCommonClusterConfiguration etcdConfigNode = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(2).withLockTimeout(1).withNodeName("main_test_node").build();
		try (IEtcdClusterService etcdClusterService = new EtcdClusterService(etcdConfigNode)) {
			etcdClusterService.init();
			etcdClusterService.getCacheManager().deleteAllCaches();
			etcdClusterService.getLockService().deleteAllLocks();
			etcdClusterService.getCoordinatorService().deleteCoordinator();
		}
	}

	private EtcdCacheManager newCacheManager() {
		final EtcdCommonClusterConfiguration etcdConfigCache = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(1).withLockTimeout(1)
				.withNodeName("AbstractEtcdTest").build();
		return new EtcdCacheManager(new EtcdClientClusterConfiguration(etcdConfigCache,
				buildEctdClient(etcdConfigCache)));
	}

	protected NodeEtcdCache newNodeEtcdCache() {
		EtcdCacheManager cacheManager = newCacheManager();
		return cacheManager.getNodeCache();
	}

}
