package fr.openwide.core.etcd.action.service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;

import fr.openwide.core.etcd.action.factory.IActionEtcdFactory;
import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;

public class EtcdActionService implements IEtcdActionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdActionService.class);

	private final IEtcdClusterService clusterService;
	private final EtcdClientClusterConfiguration config;
	private final ExecutorService executorService = Executors.newFixedThreadPool(3);
	private final IActionEtcdFactory actionFactory;

	private Watch.Watcher watcher;

	public EtcdActionService(IEtcdClusterService clusterService, EtcdClientClusterConfiguration config,
			IActionEtcdFactory actionFactory) {
		this.clusterService = clusterService;
		this.config = config;
		this.actionFactory = actionFactory;
	}

	@Override
	public String resultLessAction(AbstractEtcdActionValue action) throws EtcdServiceException {
		String uniqueID = generateUniqueActionId();
		clusterService.getCacheManager().getActionCache().put(uniqueID, action);
		LOGGER.debug("Create action {} of type {}", uniqueID, action.getClass().getSimpleName());
		return uniqueID;
	}

	@Override
	public <T> T syncedAction(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
			throws EtcdServiceException, ExecutionException, TimeoutException {
		String actionId = UUID.randomUUID().toString();
		clusterService.getCacheManager().getActionCache().put(actionId, action);
		Stopwatch stopwatch = Stopwatch.createStarted();
		while (timeout == -1 || stopwatch.elapsed(TimeUnit.MILLISECONDS) < unit.toMillis(timeout)) {
			AbstractEtcdActionValue result = clusterService.getCacheManager().getActionResultCache().get(actionId);
			if (result != null && result.isDone()) {
				try {
					@SuppressWarnings("unchecked")
					T typedResult = (T) result.getResult();
					return typedResult;
				} catch (ClassCastException e) {
					throw new ExecutionException("Type mismatch in action result", e);
				}
			} else if (result != null && result.isCancelled()) {
				throw new ExecutionException("Action was cancelled", null);
			}

			try {
				Thread.sleep(100); // Small delay to avoid busy waiting
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ExecutionException("Action execution interrupted", e);
			}
		}
		throw new TimeoutException("Action execution timed out after " + timeout + " " + unit);
	}

	@Override
	public void processAction(String actionId) throws EtcdServiceException {
		LOGGER.debug("Try to process action {}", actionId);
		AbstractEtcdActionValue action = clusterService.getCacheManager().getActionCache().get(actionId);
		if (action != null
				&& (action.broadcast() || Objects.equal(clusterService.getNodeName(), action.getTargetNode()))) {
			try {
				if (actionFactory != null) {
					actionFactory.prepareAction(action);
				}
				action.execute(clusterService);
				if (action.needsResult()) {
					clusterService.getCacheManager().getActionResultCache().put(actionId, action);
				}
			} catch (Exception e) {
				throw new EtcdServiceException("Error executing action %s".formatted(actionId), e);
			} finally {
				clusterService.getCacheManager().getActionCache().delete(actionId);
			}

		}
	}

	private String generateUniqueActionId() {
		return config.getNodeName() + "-" + UUID.randomUUID().toString();
	}

	@Override
	public void start() {
		try {
			ByteSequence prefix = ByteSequence
					.from(clusterService.getCacheManager().getActionCache().getCachePrefix().getBytes());
			WatchOption watchOption = WatchOption.builder().withPrevKV(true)
					.withRange(EtcdUtil.prefixEndOf(clusterService.getCacheManager().getActionCache().getCachePrefix()))
					.build();

			LOGGER.info("Starting watch on prefix: {}",
					clusterService.getCacheManager().getActionCache().getCachePrefix());

			watcher = config.getClient().getWatchClient().watch(prefix, watchOption, watchResponse -> {
				for (WatchEvent event : watchResponse.getEvents()) {
					if (event.getEventType() == WatchEvent.EventType.PUT) {
						String key = event.getKeyValue().getKey().toString();
						String actionId = key
								.substring(clusterService.getCacheManager().getActionCache().getCachePrefix().length());
						executorService.submit(() -> {
							try {
								LOGGER.debug("Processing action with ID: {}", actionId);
								this.processAction(actionId);
							} catch (EtcdServiceException e) {
								LOGGER.error("Error processing action {}", actionId, e);
							}
						});
					}
				}
			});

			LOGGER.info("Watch successfully started on prefix: {}",
					clusterService.getCacheManager().getActionCache().getCachePrefix());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to start watch on prefix: %s"
					.formatted(clusterService.getCacheManager().getActionCache().getCachePrefix()), e);
		}
	}

	@Override
	public void stop() {
		if (watcher != null) {
			try {
				watcher.close();
				LOGGER.info("Watch successfully stopped");
			} catch (Exception e) {
				LOGGER.error("Error while stopping watch", e);
			}
		}

		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
				LOGGER.warn("Executor service did not terminate gracefully, forcing shutdown");
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
			LOGGER.error("Interrupted while waiting for executor service to terminate", e);
		}
	}
}