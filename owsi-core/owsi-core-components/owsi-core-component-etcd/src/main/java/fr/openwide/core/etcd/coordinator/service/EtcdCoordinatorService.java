package fr.openwide.core.etcd.coordinator.service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.exception.EtcdServiceRuntimeException;
import fr.openwide.core.etcd.common.service.AbstractEtcdClientService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.PutOption;

public class EtcdCoordinatorService extends AbstractEtcdClientService implements IEtcdCoordinatorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdCoordinatorService.class);

	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	private final AtomicBoolean active = new AtomicBoolean(true);

	public EtcdCoordinatorService(EtcdClientClusterConfiguration config) {
		super(config);
	}

	@Override
	public void start() {
		active.set(true);
		// Try to become the coordinator
		tryBecomeCoordinator();
		// Watch for changes to the master key
		watchCoordinatorKey();
	}

	@Override
	public boolean tryBecomeCoordinator() {
		try {
			ByteSequence key = ByteSequence.from(getCoordinatorKey(), StandardCharsets.UTF_8);
			ByteSequence value = ByteSequence.from(getNodeName(), StandardCharsets.UTF_8);
			// Use a transaction to atomically check if the key exists and put if it doesn't
			Txn txn = getKvClient().txn()
					.If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.createRevision(0))) // Key doesn't exist
					.Then(Op.put(key, value,
							PutOption.builder().withLeaseId(getGlobalClientLeaseIdWithKeepAlive()).build()))
					.Else();
			final TxnResponse txnResponse = txn.commit().get();
			boolean success = txnResponse.isSucceeded();
			if (success) {
				LOGGER.info("Node {} is now the master", getNodeName());
			} else {
				LOGGER.debug("Node {} is not the master", getNodeName());
			}
			return success;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceRuntimeException("InterruptedException while trying become master", e);
		} catch (Exception e) {
			LOGGER.error("Error trying to become master", e);
		}
		return false;
	}

	private void watchCoordinatorKey() {
		ByteSequence key = ByteSequence.from(getCoordinatorKey(), StandardCharsets.UTF_8);
		getWatchClient().watch(key, watchResponse -> {
			if (this.getClusterConfig().isUpdateCoordinatorEnable() && active.get()
					&& !watchResponse.getEvents().isEmpty()) {
				executorService.submit(this::tryBecomeCoordinator);
			}
		});
	}
	
	@Override
	public boolean isCoordinator() {
		try {
			return Objects.equal(getCurrentCoordinator(), getNodeName());
		} catch (EtcdServiceException e) {
			LOGGER.error("Unable to check master node", e);
		}
		return false;
	}

	@Override
	public String getCurrentCoordinator() throws EtcdServiceException {
		try {
			// Get the current value of the master key
			GetResponse response = getValue(getCoordinatorKey());
			// Check if the key exists
			if (response.getKvs().isEmpty()) {
				return null; // No master elected yet
			}
			// Return the nodeName of the current master
			return response.getKvs().get(0).getValue().toString(StandardCharsets.UTF_8);

		} catch (Exception e) {
			throw new EtcdServiceException("Failed to get master node name", e);
		}

	}

	@Override
	public boolean isClusterActive() {
		try {
			// Is it enough to simply check that the master node is present ?
			return getCurrentCoordinator() != null;
		} catch (EtcdServiceException e) {
			LOGGER.error("Unable to check if cluster is active", e);
			return false;
		}
	}

	@Override
	public void stop() {
		active.set(false);
		deleteCoordinatorKeyIfOwned();
	}

	private boolean deleteCoordinatorKeyWithValue(String valueToDelete) {
		try {
			ByteSequence byteSeqValueToDelete = ByteSequence.from(valueToDelete, StandardCharsets.UTF_8);
			ByteSequence key = ByteSequence.from(getCoordinatorKey(), StandardCharsets.UTF_8);
			
			// Use a transaction to atomically check if coordinator matches and delete if it is.
			Txn txn = getKvClient().txn()
					.If(new Cmp(key, Cmp.Op.EQUAL, CmpTarget.value(byteSeqValueToDelete))) // Check if value matches
					.Then(Op.delete(key, DeleteOption.DEFAULT)).Else();

			final TxnResponse txnResponse = txn.commit().get();
			if (txnResponse.isSucceeded() && CollectionUtils.isNotEmpty(txnResponse.getDeleteResponses())) {
				LOGGER.info("Coordinator key has been deleted");
				return true;
			} 
			LOGGER.debug("Coordinator key was not present or value did not match");
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceRuntimeException("InterruptedException while trying to delete coordinator key", e);
		} catch (Exception e) {
			LOGGER.error("Error trying to delete coordinator key", e);
			return false;
		}
	}

	@Override
	public boolean deleteCoordinator() throws EtcdServiceException {
		return deleteValue(getCoordinatorKey());
	}

	@Override
	public boolean deleteCoordinatorKeyIfOwned() {
		return deleteCoordinatorKeyWithValue(getNodeName());
	}

}

