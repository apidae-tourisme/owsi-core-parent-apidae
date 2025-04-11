package fr.openwide.core.etcd.cache.model.queuedtask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;

public class QueuedTaskEtcdCacheTest extends AbstractEtcdTest {

    @Test
	public void queuedTaskTest() throws Exception {
    	
		final EtcdCommonClusterConfiguration etcdConfig = etcdConfigurationBuilderDefaultTestBuilder().build();

		String cacheName1 = "task1";
		EtcdClientClusterConfiguration clientConfiguration1 = new EtcdClientClusterConfiguration(etcdConfig,
				Client.builder().endpoints(etcdConfig.getEndpoints()).build());
		QueuedTaskEtcdCache cache1 = new QueuedTaskEtcdCache(cacheName1, clientConfiguration1);

		String cacheName2 = "task2";
		EtcdClientClusterConfiguration clientConfiguration2 = new EtcdClientClusterConfiguration(etcdConfig,
				Client.builder().endpoints(etcdConfig.getEndpoints()).build());
		QueuedTaskEtcdCache cache2 = new QueuedTaskEtcdCache(cacheName2, clientConfiguration2);

		try {
			String key1 = "123";
			QueuedTaskEtcdValue value1 = QueuedTaskEtcdValue.from(new Date());
			String key2 = "345";
			QueuedTaskEtcdValue value2 = QueuedTaskEtcdValue.from(new Date());
			String key3 = "678";
			QueuedTaskEtcdValue value3 = QueuedTaskEtcdValue.from(new Date());

			// Init caches
			cache1.ensureCacheExists();
			assertThat(cache1.getCacheNames()).containsExactlyInAnyOrder(cacheName1);
			cache2.ensureCacheExists();
			assertThat(cache1.getCacheNames()).containsExactlyInAnyOrder(cacheName1, cacheName2);

			// Put Values
			cache1.putValueInCache(key1, value1);
			cache1.putValueInCache(key2, value2);
			cache2.putValueInCache(key2, value2);
			cache2.putValueInCache(key3, value3);

			// Get values
			assertThat(cache1.getValueFromCache(key1)).isEqualTo(value1);
			assertThat(cache1.getValueFromCache(key2)).isEqualTo(value2);
			assertThat(cache2.getValueFromCache(key2)).isEqualTo(value2);
			assertThat(cache2.getValueFromCache(key3)).isEqualTo(value3);
			assertThat(cache1.getAllKeys()).containsExactlyInAnyOrder(key1, key2);
			assertThat(cache2.getAllKeys()).containsExactlyInAnyOrder(key2, key3);

			// Delete
			cache1.deleteFromCache(key2);
			assertThat(cache1.getValueFromCache(key1)).isEqualTo(value1);
			assertThat(cache1.getValueFromCache(key2)).isNull();
			assertThat(cache1.getAllKeys()).containsExactlyInAnyOrder(key1);
			assertThat(cache2.getAllKeys()).containsExactlyInAnyOrder(key2, key3);

			// Delete all key from cache1
			cache1.deleteAllCacheKeys();
			assertThat(cache1.getValueFromCache(key1)).isNull();
			assertThat(cache1.getAllKeys()).isEmpty();
			assertThat(cache2.getAllKeys()).containsExactlyInAnyOrder(key2, key3);

			// delete cache
			cache2.deleteCache();
			assertThat(cache1.getCacheNames()).containsExactlyInAnyOrder(cacheName1);
			assertThat(cache2.getAllKeys()).isEmpty();

		} finally {
			cache1.deleteCache();
			cache2.deleteCache();
		}
    }

} 