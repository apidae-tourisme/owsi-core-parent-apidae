package fr.openwide.core.jpa.more.property;

import java.util.Set;

import fr.openwide.core.spring.property.model.AbstractPropertyIds;
import fr.openwide.core.spring.property.model.ImmutablePropertyId;

public final class JpaMoreEtcdPropertyIds extends AbstractPropertyIds {
	
	private JpaMoreEtcdPropertyIds() {
	}

	public static final ImmutablePropertyId<Boolean> ETCD_ENABLED = immutable("etcd.enabled");
	public static final ImmutablePropertyId<String> ETCD_CLUSTER_NAME = immutable("etcd.clusterName");
	public static final ImmutablePropertyId<String> ETCD_NODE_NAME = immutable("etcd.nodeName");
	public static final ImmutablePropertyId<String> ETCD_ENDPOINTS = immutable("etcd.endpoints");
	public static final ImmutablePropertyId<Long> ETCD_DEFAULT_LEASE_TTL = immutable("etcd.defaultLeaseTtl");
	public static final ImmutablePropertyId<Set<String>> ETCD_ROLES = immutable("etcd.roles");

	public static final ImmutablePropertyId<Boolean> ETCD_FLUSH_STATISTIC_ENABLED = immutable(
			"etcd.flushstatistic.enabled");

}
