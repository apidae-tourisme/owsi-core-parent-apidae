package fr.openwide.core.etcd.lease.model;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.grpc.stub.StreamObserver;

public class NodeEtcdLeaseProvider implements ILeaseProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(NodeEtcdLeaseProvider.class);
	private static final long REVOKE_LEASE_TIMEOUT = 10;

	private final Client client;
	private final EtcdCommonClusterConfiguration config;
	private final AtomicBoolean isShutdown;

	private Long globalClientLeaseId = null;

	public NodeEtcdLeaseProvider(Client etcdClient, EtcdCommonClusterConfiguration config, AtomicBoolean isShutdown) {
		this.client = etcdClient;
		this.config = config;
		this.isShutdown = isShutdown;
	}

	@Override
	public Long getNewLeaseIdWithKeepAlive() throws EtcdServiceException {
		return createLeaseWithKeepAlive();
	}

	public Long getGlobalClientLeaseIdWithKeepAlive() throws EtcdServiceException {
		if (globalClientLeaseId != null) {
			return globalClientLeaseId;
		}
		globalClientLeaseId = getNewLeaseIdWithKeepAlive();
		return globalClientLeaseId;
	}

	private Long createLeaseWithKeepAlive() throws EtcdServiceException {
		LeaseGrantResponse leaseGrantResponse = createLease();
		if (leaseGrantResponse == null) {
			throw new IllegalStateException("Unable to create lease for node %s".formatted(config.getNodeName()));
		}
		// Start keep alive
		startKeepAlive(getLeaseClient(), leaseGrantResponse.getID());
		return leaseGrantResponse.getID();
	}

	protected void startKeepAlive(Lease leaseClient, long leaseId) {
		if (isShutdown.get()) {
			return;
		}

		// Set up the keep-alive stream
		leaseClient.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
			@Override
			public void onNext(LeaseKeepAliveResponse value) {
				if (value != null) {
					LOGGER.debug("Keep-alive response received by node: {} with TTL: {} seconds", config.getNodeName(),
							value.getTTL());
				} else {
					LOGGER.debug("Received null keep-alive response by node: {}", config.getNodeName());
				}
			}

			@Override
			public void onError(Throwable t) {
				if (isShutdown.get()) {
					return;
				}
				LOGGER.error("Keep-alive stream error by node: {}", config.getNodeName(), t);
			}

			@Override
			public void onCompleted() {
				LOGGER.debug("Keep-alive stream completed by node: {}", config.getNodeName());
			}
		});
	}

	protected LeaseGrantResponse createLease() throws EtcdServiceException {
		try {
			return getLeaseClient().grant(config.getLeaseTtl()).get();
		} catch (ExecutionException e) {
			throw new EtcdServiceException("ExecutionException while trying to create lease", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Interrupted while trying to create lease", e);
		}
	}

	@Override
	public LeaseRevokeResponse revokeLease(long leaseId) throws EtcdServiceException {
		try {
			return getLeaseClient().revoke(leaseId).get(REVOKE_LEASE_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Interrupted while trying to revoke a lease", e);
		} catch (Exception e) {
			LOGGER.warn("Failed to revoke lease {}", leaseId);
			return null;
		}
	}

	protected Lease getLeaseClient() {
		return client.getLeaseClient();

	}

}
