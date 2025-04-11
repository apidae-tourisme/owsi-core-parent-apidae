package fr.openwide.core.etcd.action.model;

import java.io.Serializable;

import fr.openwide.core.etcd.cache.model.IEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;

public interface IEtcdActionCacheValue extends IEtcdCacheValue {

	/**
	 * Execute the action and return its result
	 * 
	 * @return the result of the action
	 */
	ActionResultValue execute(IEtcdClusterService clusterService);

	boolean needsResult();

	boolean broadcast();

	/**
	 * @return true if the action is done
	 */
	boolean isDone();

	/**
	 * @return true if the action was cancelled
	 */
	boolean isCancelled();

	/**
	 * Get the result of the action
	 * 
	 * @return the result
	 */
	Serializable getResult();

	String getTargetNode();

}