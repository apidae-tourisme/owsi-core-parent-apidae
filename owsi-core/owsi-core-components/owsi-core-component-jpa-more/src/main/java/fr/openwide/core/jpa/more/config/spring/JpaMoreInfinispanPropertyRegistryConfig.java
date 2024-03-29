package fr.openwide.core.jpa.more.config.spring;

import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_CLUSTER_NAME;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_ENABLED;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_NODE_NAME;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_ROLES;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_ROLES_REBALANCE;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_REMOTE_TIMEOUT;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_LOCK_ACQUISITION_TIMEOUT;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_TRANSPORT_CONFIGURATION_FILE;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_TRANSPORT_PROPERTIES;
import static fr.openwide.core.jpa.more.property.JpaMoreInfinispanPropertyIds.INFINISPAN_TRANSPORT_PROPERTY;

import java.util.Collections;
import java.util.Set;

import org.springframework.context.annotation.Configuration;

import com.google.common.base.Converter;

import fr.openwide.core.commons.util.functional.Suppliers2;
import fr.openwide.core.commons.util.functional.converter.StringCollectionConverter;
import fr.openwide.core.spring.config.spring.AbstractApplicationPropertyRegistryConfig;
import fr.openwide.core.spring.property.service.IPropertyRegistry;

@Configuration
public class JpaMoreInfinispanPropertyRegistryConfig extends AbstractApplicationPropertyRegistryConfig {
	
	@Override
	protected void register(IPropertyRegistry registry) {
		registry.registerBoolean(INFINISPAN_ENABLED, false);
		registry.registerString(INFINISPAN_NODE_NAME, "node");
		registry.registerString(INFINISPAN_CLUSTER_NAME, "cluster");
		registry.registerInteger(INFINISPAN_LOCK_ACQUISITION_TIMEOUT, 60);
		registry.registerInteger(INFINISPAN_REMOTE_TIMEOUT, 60);
		// Set of roles, separated by ','
		registry.register(INFINISPAN_ROLES, new StringCollectionConverter<String, Set<String>>(Converter.identity(), Suppliers2.hashSet()).separator(","), Collections.emptySet());
		// Set of roles, separated by ','
		registry.register(INFINISPAN_ROLES_REBALANCE, new StringCollectionConverter<String, Set<String>>(Converter.identity(), Suppliers2.hashSet()).separator(","), (Set<String>) null);
		// this file is available in infinispan classpath
		registry.registerString(INFINISPAN_TRANSPORT_CONFIGURATION_FILE, "default-configs/default-jgroups-udp.xml");
		// Properties to import for transport configuration, separated by ','
		registry.register(INFINISPAN_TRANSPORT_PROPERTIES, new StringCollectionConverter<String, Set<String>>(Converter.identity(), Suppliers2.hashSet()).separator(","), Collections.emptySet());
		registry.registerString(INFINISPAN_TRANSPORT_PROPERTY);
	}

}
