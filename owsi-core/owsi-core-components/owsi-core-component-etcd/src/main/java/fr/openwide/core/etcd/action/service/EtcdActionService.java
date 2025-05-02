package fr.openwide.core.etcd.action.service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;

public class EtcdActionService implements IEtcdActionService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdActionService.class);
    
	private final IEtcdClusterService clusterService;

	private final EtcdClientClusterConfiguration config;
    
	public EtcdActionService(IEtcdClusterService clusterService, EtcdClientClusterConfiguration config) {
		this.clusterService = clusterService;
		this.config = config;
    }

	@Override
	public String resultLessAction(AbstractEtcdActionValue action) throws EtcdServiceException {
		String uniqueID = generateUniqueActionId();
		clusterService.getCacheManager().getActionCache().put(uniqueID, action);
		LOGGER.debug("Create action {} of type {}", uniqueID, action.getClass().getSimpleName());
		return uniqueID;
	}
    
	@Override
	public <T> T executeAsync(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
            throws EtcdServiceException, ExecutionException, TimeoutException {
		String actionId = UUID.randomUUID().toString();
		clusterService.getCacheManager().getActionCache().put(actionId, action);
        Stopwatch stopwatch = Stopwatch.createStarted();
        
        while (timeout == -1 || stopwatch.elapsed(TimeUnit.MILLISECONDS) < unit.toMillis(timeout)) {
			AbstractEtcdActionValue result = clusterService.getCacheManager().getActionResultCache()
					.get(actionId);
            if (result != null && result.isDone()) {
                try {
                    @SuppressWarnings("unchecked")
                    T typedResult = (T) result.get();
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
		AbstractEtcdActionValue action = clusterService.getCacheManager().getActionCache()
				.get(actionId);
		if (action != null && (action.broadcast() || Objects.equal(clusterService.getAddress(), action.getTargetNode()))) {
			try {
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
} 