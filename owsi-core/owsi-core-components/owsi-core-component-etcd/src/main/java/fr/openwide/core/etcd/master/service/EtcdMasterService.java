package fr.openwide.core.etcd.master.service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.AbstractEtcdClientService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;

public class EtcdMasterService extends AbstractEtcdClientService implements IEtcdMasterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdMasterService.class);

	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	public EtcdMasterService(EtcdClientClusterConfiguration config) {
		super(config);
	}

	@Override
	public void start() {
		// Try to become the master
		tryBecomeMaster();
		// Watch for changes to the master key
		watchMasterKey();
	}

	@Override
	public boolean tryBecomeMaster() {
		try {
			ByteSequence key = ByteSequence.from(getMasterKey(), StandardCharsets.UTF_8);
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
		} catch (Exception e) {
			LOGGER.error("Error trying to become master", e);
		}
		return false;
	}

	private void watchMasterKey() {
		ByteSequence key = ByteSequence.from(getMasterKey(), StandardCharsets.UTF_8);
		getWatchClient().watch(key, watchResponse -> {
			if (!watchResponse.getEvents().isEmpty()) {
				executorService.submit(this::tryBecomeMaster);
			}
		});
	}
	
	@Override
	public boolean isMaster() {
		try {
			return Objects.equal(getCurrentMaster(), getNodeName());
		} catch (EtcdServiceException e) {
			LOGGER.error("Unable to check master node", e);
		}
		return false;
	}

	@Override
	public String getCurrentMaster() throws EtcdServiceException {
		ByteSequence key = ByteSequence.from(getMasterKey(), StandardCharsets.UTF_8);
		// Get the current value of the master key
		GetResponse response;
		try {
			response = getKvClient().get(key).get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EtcdServiceException("Failed to get master node name", e);
		} catch (TimeoutException e) {
			throw new EtcdServiceException("Timeout while getting master node name", e);
		} catch (Exception e) {
			throw new EtcdServiceException("Failed to get master node name", e);
		}

		// Check if the key exists
		if (response.getKvs().isEmpty()) {
			return null; // No master elected yet
		}
		// Return the nodeName of the current master
		return response.getKvs().get(0).getValue().toString(StandardCharsets.UTF_8);
	}

}

