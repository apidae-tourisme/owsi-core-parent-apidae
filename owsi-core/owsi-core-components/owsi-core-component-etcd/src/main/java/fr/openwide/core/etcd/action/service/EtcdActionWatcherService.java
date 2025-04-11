package fr.openwide.core.etcd.action.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.cache.model.action.ActionEtcdCache;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;

public class EtcdActionWatcherService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdActionWatcherService.class);
    
    private final EtcdActionService actionService;
	private final ActionEtcdCache actionCache;
	private final EtcdClientClusterConfiguration config;
    private Watch.Watcher watcher;

	private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    
	public EtcdActionWatcherService(EtcdActionService actionService, ActionEtcdCache actionCache,
			EtcdClientClusterConfiguration config) {
		this.config = config;
		this.actionService = actionService;
		this.actionCache = actionCache;
    }
    
    public void start() {
        try {
            ByteSequence prefix = ByteSequence.from(actionCache.getCachePrefix().getBytes());
			WatchOption watchOption = WatchOption.builder()
					.withPrevKV(true)
					.withRange(EtcdUtil.prefixEndOf(actionCache.getCachePrefix())).build();
            
            LOGGER.info("Starting watch on prefix: {}", actionCache.getCachePrefix());
            
            watcher = config.getClient().getWatchClient().watch(prefix, watchOption, watchResponse -> {
                for (WatchEvent event : watchResponse.getEvents()) {
                    if (event.getEventType() == WatchEvent.EventType.PUT) {
                        String key = event.getKeyValue().getKey().toString();
                        String actionId = key.substring(actionCache.getCachePrefix().length());
						executorService.submit(() -> {
							try {
								LOGGER.debug("Processing action with ID: {}", actionId);
								actionService.processAction(actionId);
							} catch (EtcdServiceException e) {
								LOGGER.error("Error processing action {}", actionId, e);
							}
						});
                    }
                }
            });
            
			LOGGER.info("Watch successfully started on prefix: {}", actionCache.getCachePrefix());
        } catch (Exception e) {
			throw new IllegalStateException(
					"Failed to start watch on prefix: %s".formatted(actionCache.getCachePrefix()), e);
        }
    }
    
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