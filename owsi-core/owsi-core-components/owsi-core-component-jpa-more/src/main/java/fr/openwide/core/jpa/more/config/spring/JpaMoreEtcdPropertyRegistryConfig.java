package fr.openwide.core.jpa.more.config.spring;

import java.util.Collections;
import java.util.Set;

import org.springframework.context.annotation.Configuration;

import com.google.common.base.Converter;

import fr.openwide.core.commons.util.functional.Suppliers2;
import fr.openwide.core.commons.util.functional.converter.StringCollectionConverter;
import fr.openwide.core.jpa.more.property.JpaMoreEtcdPropertyIds;
import fr.openwide.core.spring.config.spring.AbstractApplicationPropertyRegistryConfig;
import fr.openwide.core.spring.property.service.IPropertyRegistry;

@Configuration
public class JpaMoreEtcdPropertyRegistryConfig extends AbstractApplicationPropertyRegistryConfig {
	
	@Override
	protected void register(IPropertyRegistry registry) {
		registry.registerBoolean(JpaMoreEtcdPropertyIds.ETCD_ENABLED, false);
		registry.registerBoolean(JpaMoreEtcdPropertyIds.ETCD_FLUSH_STATISTIC_ENABLED, false);
		registry.registerString(JpaMoreEtcdPropertyIds.ETCD_NODE_NAME, "node");
		registry.registerString(JpaMoreEtcdPropertyIds.ETCD_CLUSTER_NAME, "cluster");
		registry.registerString(JpaMoreEtcdPropertyIds.ETCD_ENDPOINTS, "http://localhost:2379");
		registry.registerLong(JpaMoreEtcdPropertyIds.ETCD_DEFAULT_LEASE_TTL, 10L);
		// Set of roles, separated by ','
		registry.register(JpaMoreEtcdPropertyIds.ETCD_ROLES,
				new StringCollectionConverter<String, Set<String>>(Converter.identity(), Suppliers2.hashSet())
						.separator(","),
				Collections.emptySet());
	}

}
