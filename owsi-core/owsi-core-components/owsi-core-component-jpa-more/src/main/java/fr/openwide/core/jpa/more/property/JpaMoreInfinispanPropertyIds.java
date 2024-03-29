package fr.openwide.core.jpa.more.property;

import java.util.Objects;
import java.util.Set;

import fr.openwide.core.spring.property.model.AbstractPropertyIds;
import fr.openwide.core.spring.property.model.ImmutablePropertyId;
import fr.openwide.core.spring.property.model.ImmutablePropertyIdTemplate;

public final class JpaMoreInfinispanPropertyIds extends AbstractPropertyIds {
	
	private JpaMoreInfinispanPropertyIds() {
	}
	
	/*
	 * Immutable Properties
	 */
	public static final ImmutablePropertyId<Boolean> INFINISPAN_ENABLED = immutable("infinispan.enabled");
	public static final ImmutablePropertyId<String> INFINISPAN_CLUSTER_NAME = immutable("infinispan.clusterName");
	public static final ImmutablePropertyId<String> INFINISPAN_NODE_NAME = immutable("infinispan.nodeName");
	public static final ImmutablePropertyId<Integer> INFINISPAN_LOCK_ACQUISITION_TIMEOUT = immutable("infinispan.lockAcquisitionTimeout");
	public static final ImmutablePropertyId<Integer> INFINISPAN_REMOTE_TIMEOUT = immutable("infinispan.remoteTimeout");
	/**
	 * Complete list of roles
	 */
	public static final ImmutablePropertyId<Set<String>> INFINISPAN_ROLES = immutable("infinispan.roles");
	/**
	 * Roles allowed to be captured by node on rebalance (allow to statically assign roles on some nodes)
	 */
	public static final ImmutablePropertyId<Set<String>> INFINISPAN_ROLES_REBALANCE = immutable("infinispan.rolesRebalance");
	public static final ImmutablePropertyId<String> INFINISPAN_TRANSPORT_CONFIGURATION_FILE = immutable("infinispan.transport.configurationFile");
	public static final ImmutablePropertyId<Set<String>> INFINISPAN_TRANSPORT_PROPERTIES = immutable("infinispan.transport.properties");
	public static final ImmutablePropertyIdTemplate<String> INFINISPAN_TRANSPORT_PROPERTY = immutableTemplate("infinispan.transport.property.%1s");
	public static final ImmutablePropertyId<String> transportProperty(String transportPropertyKey) {
		Objects.requireNonNull(transportPropertyKey);
		return INFINISPAN_TRANSPORT_PROPERTY.create(transportPropertyKey);
	}
}
