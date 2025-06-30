package fr.openwide.core.etcd.common.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;

public class AbstractEtcdClientService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEtcdClientService.class);

	private final EtcdClientClusterConfiguration config;

	public AbstractEtcdClientService(EtcdClientClusterConfiguration config) {
		this.config = config;
	}

	protected KV getKvClient() {
		return config.getClient().getKVClient();
	}

	protected Lock getLockClient() {
		return config.getClient().getLockClient();
	}

	protected Lease getLeaseClient() {
		return config.getClient().getLeaseClient();
	}

	protected Watch getWatchClient() {
		return config.getClient().getWatchClient();
	}

	protected Watch get() {
		return config.getClient().getWatchClient();
	}

	protected GetResponse getValue(String key) throws EtcdServiceException {
		CompletableFuture<GetResponse> getFuture = getKvClient().get(ByteSequence.from(key.getBytes()));
		try {
			return getFuture.get(config.getClusterConfiguration().getTimeout(), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get value for key: " + key, e);
		} catch (Exception e) {
			throw new EtcdServiceException("Exception while getting value for key: " + key, e);
		}
	}

	protected boolean deleteValue(String key) throws EtcdServiceException {
		try {
			CompletableFuture<DeleteResponse> deleteFuture = getKvClient().delete(ByteSequence.from(key.getBytes()));
			DeleteResponse response = deleteFuture.get();
			return response.getDeleted() > 0;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to delete key" + key, e);
		} catch (ExecutionException e) {
			throw new EtcdServiceException("Failed to delete key" + key, e);
		}
	}

	protected List<KeyValue> getAllFromPrefix(String prefix) throws EtcdServiceException {
		CompletableFuture<GetResponse> getFuture = getKvClient().get(ByteSequence.from(prefix, StandardCharsets.UTF_8),
				GetOption.builder().withRange(EtcdUtil.prefixEndOf(prefix)).build());
		try {
			return getFuture.get(config.getClusterConfiguration().getTimeout(), TimeUnit.SECONDS).getKvs();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get value from prefix: " + prefix, e);
		} catch (Exception e) {
			throw new EtcdServiceException("Exception while getting value from prefix: " + prefix, e);
		}

	}

	protected Set<String> getAllKeysFromPrefix(String prefix) throws EtcdServiceException {
		List<KeyValue> keyValues = getAllFromPrefix(prefix);
		if (keyValues.isEmpty()) {
			return Set.of();
		}

		return keyValues.stream()
				.map(kv -> new String(kv.getKey().getBytes(), StandardCharsets.UTF_8).substring(prefix.length()))
				.collect(Collectors.toSet());
	}

	protected DeleteResponse deleteAllKeysFromPrefix(String prefix) throws InterruptedException, ExecutionException {
		CompletableFuture<DeleteResponse> deleteFuture = getKvClient().delete(
				ByteSequence.from(prefix, StandardCharsets.UTF_8),
				DeleteOption.builder().withRange(EtcdUtil.prefixEndOf(prefix)).build());
		return deleteFuture.get();
	}

	protected LeaseRevokeResponse revokeLease(long leaseId) throws EtcdServiceException {
		return config.getLeaseProvider().revokeLease(leaseId);
	}

	protected Long getGlobalClientLeaseIdWithKeepAlive() throws EtcdServiceException {
		return config.getLeaseProvider().getGlobalClientLeaseIdWithKeepAlive();
	}

	protected Long getNewLeaseIdWithKeepAlive() throws EtcdServiceException {
		return config.getLeaseProvider().getNewLeaseIdWithKeepAlive();
	}

	protected EtcdCommonClusterConfiguration getClusterConfig() {
		return config.getClusterConfiguration();
	}

	protected String getNodeName() {
		return config.getNodeName();
	}

}
