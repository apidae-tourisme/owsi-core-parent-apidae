package fr.openwide.core.etcd.cache.model.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;

public class RoleEtcdCacheTest extends AbstractEtcdTest {

    @Test
	public void cleanCacheFromNodeTest() throws Exception {
    	
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder().build();
		String cacheName = "roleCacheTest";
		EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
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

			roleCache.put(role1, valueRole1);
			roleCache.put(role2, valueRole2);
			roleCache.put(role3, valueRole3);

			assertThat(roleCache.getAllKeys()).as("toutes les roles sont ajoutés").containsExactlyInAnyOrder(role1,
					role2, role3);

			roleCache.cleanCacheFromNode(node1);

			assertThat(roleCache.getAllKeys()).as("Les roles du node1 sont supprimés").containsExactlyInAnyOrder(role3);

		} finally {
			roleCache.deleteCache();
		}
    }

	@Test
	public void putIfAbsentTest() throws Exception {

		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder().build();
		String cacheName = "roleCacheTest";
		EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		RoleEtcdCache roleCache = new RoleEtcdCache(cacheName, clientConfiguration2);

		String role1 = "role1";

		String node1 = "node1";
		String node2 = "node2";
		RoleEtcdValue valueRole1 = RoleEtcdValue.from(new Date(), node1);
		RoleEtcdValue valueRole2 = RoleEtcdValue.from(new Date(), node2);

		final RoleEtcdValue putIfAbsent = roleCache.putIfAbsent(role1, valueRole1);
		assertThat(putIfAbsent).as("Null because key was absent").isNull();
		assertThat(roleCache.get(role1)).isNotNull();
		assertThat(roleCache.get(role1).getNodeName()).as("key %s is present".formatted(role1))
				.isEqualTo(node1);

		final RoleEtcdValue putIfAbsentAgain = roleCache.putIfAbsent(role1, valueRole2);
		assertThat(putIfAbsentAgain).as("Not Null, key was already present").isNotEqualTo(valueRole2)
				.isEqualTo(valueRole1);
	}

	@Test
	public void deleteIfNodeMatchesTest() throws Exception {

		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder().build();
		String cacheName = "roleCacheTest";
		EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				buildEctdClient(etcdConfig));
		RoleEtcdCache roleCache = new RoleEtcdCache(cacheName, clientConfiguration2);

		String role1 = "role1";
		String node1 = "node1";
		RoleEtcdValue valueRole1 = RoleEtcdValue.from(new Date(), node1);

		String role2 = "role2";
		String node2 = "node2";

		roleCache.put(role1, valueRole1);

		assertThat(roleCache.getAllKeys()).singleElement().isEqualTo(role1);

		assertThat(roleCache.deleteIfNodeMatches(role2, node1)).as("role2 key not exists").isFalse();
		assertThat(roleCache.deleteIfNodeMatches(role1, node2)).as("role1 key does not match node2").isFalse();
		assertThat(roleCache.deleteIfNodeMatches(role1, node1)).as("role1 matches node1").isTrue();

		assertThat(roleCache.getAllKeys()).as("all keys are deleted").isEmpty();

	}

} 