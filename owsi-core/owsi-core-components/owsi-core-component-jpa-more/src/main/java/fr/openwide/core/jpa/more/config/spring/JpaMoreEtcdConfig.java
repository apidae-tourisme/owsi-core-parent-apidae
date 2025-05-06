package fr.openwide.core.jpa.more.config.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fr.openwide.core.etcd.action.factory.ActionEtcdFactory;
import fr.openwide.core.etcd.action.factory.IActionEtcdFactory;
import fr.openwide.core.etcd.common.service.EtcdClusterService;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.infinispan.service.IRolesProvider;
import fr.openwide.core.infinispan.utils.role.RolesFromStringSetProvider;
import fr.openwide.core.jpa.more.etcd.service.EtcdQueueTaskManagerServiceImpl;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;
import fr.openwide.core.jpa.more.property.JpaMoreEtcdPropertyIds;
import fr.openwide.core.spring.property.service.IPropertyService;

@Configuration
@Import({
		JpaMoreEtcdPropertyRegistryConfig.class
})
public class JpaMoreEtcdConfig {

	public static final String ETCD_ROLE_PROVIDER = "etcdRolesProvider";

	@Bean(name = ETCD_ROLE_PROVIDER)
	public IRolesProvider etcdRolesProvider(IPropertyService propertyService) {
		if (Boolean.FALSE.equals(propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ENABLED))) {
			return null;
		}
		return new RolesFromStringSetProvider(propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ROLES),
				propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ROLES));
	}

	@Bean
	public IActionEtcdFactory actionEtcdFactory() {
		return new ActionEtcdFactory();
	}

	@Bean(destroyMethod = "stop")
	public IEtcdClusterService etcdClusterService(IPropertyService propertyService,
			@Autowired(required = false) @Qualifier(value = ETCD_ROLE_PROVIDER) IRolesProvider rolesProvider,
			IActionEtcdFactory actionFactory) {
		if (Boolean.FALSE.equals(propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ENABLED))) {
			return null;
		}
		String nodeName = propertyService.get(JpaMoreEtcdPropertyIds.ETCD_NODE_NAME);
		String endpoints = propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ENDPOINTS);
		String clusterName = propertyService.get(JpaMoreEtcdPropertyIds.ETCD_CLUSTER_NAME);
		long leaseTtl = propertyService.get(JpaMoreEtcdPropertyIds.ETCD_DEFAULT_LEASE_TTL);
		
		EtcdCommonClusterConfiguration conf = EtcdCommonClusterConfiguration.builder()
				.withEndpoints(endpoints)
				.withNodeName(nodeName)
				.withClusterName(clusterName)
				.withLeaseTtl(leaseTtl)
				.withRoleProvider(rolesProvider)
				.withActionFactory(actionFactory)
				.build();

		IEtcdClusterService clusterService = new EtcdClusterService(conf);
		clusterService.init();
		return clusterService;
	}

	@Bean
	public IEtcdQueueTaskManagerService etcdQueueTaskManagerService(IPropertyService propertyService) {
		if (Boolean.TRUE.equals(propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ENABLED))) {
			return new EtcdQueueTaskManagerServiceImpl();
		}
		return null;
	}

}
