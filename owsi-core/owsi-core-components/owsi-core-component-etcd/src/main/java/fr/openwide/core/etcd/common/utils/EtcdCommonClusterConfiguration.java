package fr.openwide.core.etcd.common.utils;

import fr.openwide.core.etcd.action.factory.IActionEtcdFactory;
import fr.openwide.core.infinispan.service.IRolesProvider;

public class EtcdCommonClusterConfiguration {

	private final String endpoints;

	private final String nodeName;

	private final String clusterName;

	private final long leaseTtl;

	private final long lockTimeout;

	private final long timeout;

	private final IRolesProvider roleProvider;

	private final boolean roleRebalanceEnable;

	private final boolean updateCoordinatorEnable;

	private final int connectTimeout;
	
	private final IActionEtcdFactory actionFactory;

	private EtcdCommonClusterConfiguration(EtcdConfigurationBuilder builder) {
		this.endpoints = builder.endpoints;
		this.nodeName = builder.nodeName;
		this.clusterName = builder.clusterName;
		this.leaseTtl = builder.leaseTtl;
		this.lockTimeout = builder.lockTimeout;
		this.timeout = builder.timeout;
		this.roleProvider = builder.roleProvider;
		this.roleRebalanceEnable = builder.roleRebalanceEnable;
		this.updateCoordinatorEnable = builder.updateCoordinatorEnable;
		this.connectTimeout = builder.connectTimeout;
		this.actionFactory = builder.actionFactory;
	}

	public String getEndpoints() {
		return endpoints;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getClusterName() {
		return clusterName;
	}

	public static EtcdConfigurationBuilder builder() {
		return new EtcdConfigurationBuilder();
	}

	public long getLeaseTtl() {
		return leaseTtl;
	}

	public long getLockTimeout() {
		return lockTimeout;
	}

	public long getTimeout() {
		return timeout;
	}

	public IRolesProvider getRoleProvider() {
		return roleProvider;
	}

	public boolean isRoleRebalanceEnable() {
		return roleRebalanceEnable;
	}

	public boolean isUpdateCoordinatorEnable() {
		return updateCoordinatorEnable;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}
	
	public IActionEtcdFactory getActionFactory() {
		return actionFactory;
	}

	public static class EtcdConfigurationBuilder {
		private String endpoints;

		private String nodeName;

		private String clusterName;

		private long leaseTtl = 30;

		private long lockTimeout = 10;

		private long timeout = 10;

		private IRolesProvider roleProvider;

		private boolean roleRebalanceEnable = true;

		private boolean updateCoordinatorEnable = true;

		private int connectTimeout = 15;
		
		private IActionEtcdFactory actionFactory;

		private EtcdConfigurationBuilder() {
		}

		public EtcdConfigurationBuilder withEndpoints(String endpoints) {
			this.endpoints = endpoints;
			return this;
		}

		public EtcdConfigurationBuilder withNodeName(String nodeName) {
			this.nodeName = nodeName;
			return this;
		}

		public EtcdConfigurationBuilder withClusterName(String clusterName) {
			this.clusterName = clusterName;
			return this;
		}

		public EtcdConfigurationBuilder withLeaseTtl(long leaseTtl) {
			this.leaseTtl = leaseTtl;
			return this;
		}

		public EtcdConfigurationBuilder withRoleProvider(IRolesProvider roleProvider) {
			this.roleProvider = roleProvider;
			return this;
		}

		public EtcdConfigurationBuilder withLockTimeout(long lockTimeout) {
			this.lockTimeout = lockTimeout;
			return this;
		}

		public EtcdConfigurationBuilder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		public EtcdConfigurationBuilder withRoleRebalanceEnable(boolean roleRebalanceEnable) {
			this.roleRebalanceEnable = roleRebalanceEnable;
			return this;
		}

		public EtcdConfigurationBuilder withUpdateCoordinatorEnable(boolean updateCoordinatorEnable) {
			this.updateCoordinatorEnable = updateCoordinatorEnable;
			return this;
		}

		public EtcdConfigurationBuilder withConnectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}
		
		public EtcdConfigurationBuilder withActionFactory(IActionEtcdFactory actionFactory) {
			this.actionFactory = actionFactory;
			return this;
		}

		public EtcdCommonClusterConfiguration build() {
			return new EtcdCommonClusterConfiguration(this);
		}
	}
}