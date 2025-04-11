package fr.openwide.core.etcd.cache.model.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class RoleEtcdCacheTest extends AbstractEtcdTest {

    @Test
	public void cleanCacheFromNodeTest() throws Exception {
    	
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder().build();
		String cacheName = "roleCacheTest";
		EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				Client.builder().endpoints(etcdConfig.getEndpoints()).build());
		RoleEtcdCache roleCache = new RoleEtcdCache(cacheName, clientConfiguration2);
		try {

			String node1 = "node1";
			String node2 = "node2";

			String role1 = "role1";
			RoleEtcdValue valueRole1 = RoleEtcdValue.from(new Date(), node1);
			String role2 = "role2";
			RoleEtcdValue valueRole2 = RoleEtcdValue.from(new Date(), node1);
			String role3 = "role3";
			RoleEtcdValue valueRole3 = RoleEtcdValue.from(new Date(), node2);

			roleCache.putValueInCache(role1, valueRole1);
			roleCache.putValueInCache(role2, valueRole2);
			roleCache.putValueInCache(role3, valueRole3);

			assertThat(roleCache.getAllKeys()).as("toutes les roles sont ajoutés").containsExactlyInAnyOrder(role1,
					role2, role3);

			roleCache.cleanCacheFromNode(node1);

			assertThat(roleCache.getAllKeys()).as("Les roles du node1 sont supprimés").containsExactlyInAnyOrder(role3);

		} finally {
			roleCache.deleteCache();
		}
    }

} 