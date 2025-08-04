package fr.openwide.core.etcd.lease.model;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
	private static final int MAX_CONSECUTIVE_ERRORS = 3;

	private final Client client;
	private final EtcdCommonClusterConfiguration config;
	private final AtomicBoolean isShutdown;
	
	// Thread-safe reference to the global lease ID and its last keep-alive timestamp
	private final AtomicReference<LeaseInfo> globalLeaseInfo = new AtomicReference<>();

	public NodeEtcdLeaseProvider(Client etcdClient, EtcdCommonClusterConfiguration config, AtomicBoolean isShutdown) {
		this.client = etcdClient;
		this.config = config;
		this.isShutdown = isShutdown;
	}

	@Override
	public Long getNewLeaseIdWithKeepAlive() throws EtcdServiceException {
		return createLeaseWithKeepAlive();
	}

	@Override
	public Long getGlobalClientLeaseIdWithKeepAlive() throws EtcdServiceException {
		LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
		
		// If no lease exists or current lease is expired, create a new one
		if (currentLeaseInfo == null || currentLeaseInfo.isExpired(config.getLeaseTtl())) {
			return createOrReuseGlobalLease(currentLeaseInfo);
		}
		
		return currentLeaseInfo.getLeaseId();
	}

	private synchronized Long createOrReuseGlobalLease(LeaseInfo expiredLeaseInfo) throws EtcdServiceException {
		// Double-check: another thread might have already created a new lease
		LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
		if (currentLeaseInfo != null && currentLeaseInfo != expiredLeaseInfo && 
			!currentLeaseInfo.isExpired(config.getLeaseTtl())) {
			LOGGER.debug("Another thread created lease {}, using that instead", currentLeaseInfo.getLeaseId());
			return currentLeaseInfo.getLeaseId();
		}
		
		LOGGER.info("Creating new lease for node {} (previous lease: {})", 
			config.getNodeName(), currentLeaseInfo != null ? currentLeaseInfo.getLeaseId() : "none");
		
		try {
			LeaseInfo newLeaseInfo = new LeaseInfo(createLeaseWithKeepAlive());
			globalLeaseInfo.set(newLeaseInfo);
			LOGGER.info("Successfully created new lease {} for node {}", newLeaseInfo.getLeaseId(), config.getNodeName());
			return newLeaseInfo.getLeaseId();
		} catch (Exception e) {
			LOGGER.error("Failed to create new lease for node {}", config.getNodeName());
			// Clear the lease info to force retry on next call
			globalLeaseInfo.set(null);
			throw e;
		}
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
					LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
					if (currentLeaseInfo != null && currentLeaseInfo.getLeaseId() == leaseId) {
						currentLeaseInfo.updateKeepAliveTime();
					}
					LOGGER.debug("Keep-alive response received by node: {} with TTL: {} seconds", config.getNodeName(),
							value.getTTL());
				} else {
					LOGGER.debug("Received null keep-alive response by node: {}", config.getNodeName());
					handleKeepAliveError(leaseId);
				}
			}

			@Override
			public void onError(Throwable t) {
				if (isShutdown.get()) {
					return;
				}
				
				boolean isLeaseNotFound = t.getMessage() != null && 
					t.getMessage().toLowerCase().contains("requested lease not found");
				
				if (isLeaseNotFound) {
					LOGGER.error("Keep-alive stream error by node: {} - Lease not found, invalidating immediately", 
						config.getNodeName(), t);
					// Immediately invalidate the lease for "lease not found" errors
					LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
					if (currentLeaseInfo != null && currentLeaseInfo.getLeaseId() == leaseId) {
						globalLeaseInfo.set(null);
					}
				} else {
					LOGGER.error("Keep-alive stream error by node: {}", config.getNodeName(), t);
					handleKeepAliveError(leaseId);
				}
			}

			@Override
			public void onCompleted() {
				LOGGER.debug("Keep-alive stream completed by node: {}", config.getNodeName());
				// On completion, clear the lease info to force creation of a new lease
				globalLeaseInfo.set(null);
			}
		});
	}

	private void handleKeepAliveError(long leaseId) {
		LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
		if (currentLeaseInfo != null && currentLeaseInfo.getLeaseId() == leaseId) {
			currentLeaseInfo.incrementErrorCount();
			
			LOGGER.warn("Keep-alive error for lease {} on node {}, consecutive errors: {}", 
				leaseId, config.getNodeName(), currentLeaseInfo.getErrorCount());

			if (currentLeaseInfo.getErrorCount() >= MAX_CONSECUTIVE_ERRORS) {
				LOGGER.warn("Too many consecutive keep-alive errors ({}), invalidating lease {} for node {}", 
					currentLeaseInfo.getErrorCount(), leaseId, config.getNodeName());
				globalLeaseInfo.set(null);
			}
		}
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
			// Clear the global lease info if we're revoking the global lease
			LeaseInfo currentLeaseInfo = globalLeaseInfo.get();
			if (currentLeaseInfo != null && currentLeaseInfo.getLeaseId() == leaseId) {
				globalLeaseInfo.set(null);
			}
			
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

	// Inner class to hold lease information
	private static class LeaseInfo {
		private final long leaseId;
		private final AtomicLong lastKeepAliveTime;
		private final AtomicInteger consecutiveErrors;

		public LeaseInfo(long leaseId) {
			this.leaseId = leaseId;
			this.lastKeepAliveTime = new AtomicLong(System.currentTimeMillis());
			this.consecutiveErrors = new AtomicInteger(0);
		}

		public long getLeaseId() {
			return leaseId;
		}

		public void updateKeepAliveTime() {
			lastKeepAliveTime.set(System.currentTimeMillis());
			// Reset error count on successful keep-alive
			consecutiveErrors.set(0);
		}

		public void incrementErrorCount() {
			consecutiveErrors.incrementAndGet();
		}

		public int getErrorCount() {
			return consecutiveErrors.get();
		}

		public boolean isExpired(long leaseTtlSeconds) {
			long currentTime = System.currentTimeMillis();
			long lastKeepAlive = lastKeepAliveTime.get();
			// Consider lease expired if we haven't received a keep-alive in more than 80% of TTL
			// This provides a safety margin for network issues and server processing delays
			long expirationThreshold = (long) (leaseTtlSeconds * 1000 * 0.8);
			return (currentTime - lastKeepAlive) > expirationThreshold;
		}
	}
}
