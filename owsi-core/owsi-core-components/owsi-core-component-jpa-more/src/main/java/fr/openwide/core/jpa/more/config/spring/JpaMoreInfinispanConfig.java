package fr.openwide.core.jpa.more.config.spring;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fr.openwide.core.infinispan.service.CustomConfigurationBuilderHolder;
import fr.openwide.core.infinispan.service.IActionFactory;
import fr.openwide.core.infinispan.service.IInfinispanClusterCheckerService;
import fr.openwide.core.infinispan.service.IInfinispanClusterService;
import fr.openwide.core.infinispan.service.IRolesProvider;
import fr.openwide.core.infinispan.service.InfinispanClusterServiceImpl;
import fr.openwide.core.infinispan.utils.GlobalDefaultReplicatedTransientConfigurationBuilder;
import fr.openwide.core.infinispan.utils.role.RolesFromStringSetProvider;
import fr.openwide.core.jpa.more.config.spring.util.SpringActionFactory;
import fr.openwide.core.jpa.more.infinispan.service.IInfinispanQueueTaskManagerService;
import fr.openwide.core.jpa.more.infinispan.service.InfinispanClusterJdbcCheckerServiceImpl;
import fr.openwide.core.jpa.more.infinispan.service.InfinispanQueueTaskManagerServiceImpl;
import fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds;
import fr.openwide.core.spring.property.service.IPropertyService;

@Configuration
@Import({
	JpaMoreInfinispanPropertyRegistryConfig.class
})
public class JpaMoreInfinispanConfig {

	@Bean
	public IRolesProvider rolesProvider(IPropertyService propertyService) {
		// FT - allow bean override
		if (propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ENABLED)) {
			return new RolesFromStringSetProvider(
					propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ROLES),
					propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ROLES_REBALANCE));
		} else {
			return null;
		}
	}

	@Bean
	public IActionFactory actionFactory() {
		return new SpringActionFactory();
	}

	@Bean
	public IInfinispanClusterCheckerService infinispanClusterCheckerService(IPropertyService propertyService) {
		if (propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ENABLED)) {
			return new InfinispanClusterJdbcCheckerServiceImpl();
		}
		return null;
	}

	@Bean(destroyMethod = "stop")
	public IInfinispanClusterService infinispanClusterService(IPropertyService propertyService, @Autowired(required = false) IRolesProvider rolesProvider,
			IActionFactory springActionFactory, @Autowired(required = false) IInfinispanClusterCheckerService infinispanClusterCheckerService,
			EntityManagerFactory entityManagerFactory) {
		if (propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ENABLED)) {
			String nodeName = propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_NODE_NAME);
			Properties properties = new Properties();
			for (String key : propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_TRANSPORT_PROPERTIES)) {
				properties.put(key, propertyService.getAsString(JpaMoreInfinispanPropertyIds.transportProperty(key)));
			}
			CustomConfigurationBuilderHolder holder = new CustomConfigurationBuilderHolder(properties, 
					propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_LOCK_ACQUISITION_TIMEOUT));
			GlobalDefaultReplicatedTransientConfigurationBuilder globalConfiguration =
					holder.getGlobalConfigurationBuilder();
			globalConfiguration.nodeName(nodeName);
			holder.newConfigurationBuilder("*");
			EmbeddedCacheManager cacheManager = new DefaultCacheManager(holder, false);
			
			InfinispanClusterServiceImpl cluster =
					new InfinispanClusterServiceImpl(nodeName, cacheManager, rolesProvider, springActionFactory,
							infinispanClusterCheckerService);
			cluster.init();
			return cluster;
		} else {
			return null;
		}
	}

	@Bean
	public IInfinispanQueueTaskManagerService infinispanQueueTaskManagerService(IPropertyService propertyService) {
		if (propertyService.get(JpaMoreInfinispanPropertyIds.INFINISPAN_ENABLED)) {
			return new InfinispanQueueTaskManagerServiceImpl();
		}
		return null;
	}

}
