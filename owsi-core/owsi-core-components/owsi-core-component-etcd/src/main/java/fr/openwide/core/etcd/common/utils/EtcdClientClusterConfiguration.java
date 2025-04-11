package fr.openwide.core.etcd.common.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.openwide.core.etcd.lease.model.ILeaseProvider;
import fr.openwide.core.etcd.lease.model.NodeEtcdLeaseProvider;
import io.etcd.jetcd.Client;

public class EtcdClientClusterConfiguration {

	private final EtcdCommonClusterConfiguration clusterConfiguration;

	private final Client client;

	private final ILeaseProvider leaseProvider;

	private final AtomicBoolean isShutdown;

	public EtcdClientClusterConfiguration(EtcdCommonClusterConfiguration clusterConfiguration, Client client) {
		this.clusterConfiguration = clusterConfiguration;
		this.client = client;
		this.isShutdown = new AtomicBoolean(false);
		this.leaseProvider = new NodeEtcdLeaseProvider(client, clusterConfiguration, isShutdown);
	}

	public ILeaseProvider getLeaseProvider() {
		return leaseProvider;
	}

	public Client getClient() {
		return client;
	}

	public EtcdCommonClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	public String getNodeName() {
		return clusterConfiguration.getNodeName();
	}

	public AtomicBoolean getIsShutdown() {
		return isShutdown;
	}

}