package fr.openwide.core.test.infinispan.util;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.MDC;

import fr.openwide.core.infinispan.service.CustomConfigurationBuilderHolder;
import fr.openwide.core.infinispan.utils.GlobalDefaultReplicatedTransientConfigurationBuilder;

public class TestCacheManagerBuilder {

	public volatile static String PROCESS_ID = null;

	private final String name;

	private final String taskName;

	public TestCacheManagerBuilder(String name, String taskName) {
		this.name = name;
		this.taskName = taskName;
	}

	public EmbeddedCacheManager build() {
		PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
		MDC.put("PID", PROCESS_ID);
		
		// udp + multicast is difficult to obtain in test environment
		Properties properties = new Properties();
		properties.put("configurationFile", "test-jgroups-tcp.xml");
		
		CustomConfigurationBuilderHolder holder = new CustomConfigurationBuilderHolder(properties, 60);
		GlobalDefaultReplicatedTransientConfigurationBuilder globalConfiguration = holder.getGlobalConfigurationBuilder();
		globalConfiguration.nodeName(name);
		holder.newConfigurationBuilder("*");
		EmbeddedCacheManager cacheManager = new DefaultCacheManager(holder, false);
		cacheManager.getCache(TestConstants.CACHE_DEFAULT);
		
		if (taskName != null) {
			try {
				Class<?> clazz = Class.forName(taskName);
				Constructor<?> constructor = clazz.getConstructor(EmbeddedCacheManager.class);
				Runnable r = (Runnable) constructor.newInstance(cacheManager);
				
				Thread t = new Thread((Runnable) r);
				t.start();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		
		return cacheManager;
	}

}
