package fr.openwide.core.etcd.action.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;

public abstract class AbstractEtcdActionValue extends AbstractEtcdCacheValue implements IEtcdActionCacheValue {

	private static final long serialVersionUID = 8439400632230136609L;

	protected transient IEtcdClusterService etcdClusterService;

	private final AtomicBoolean done = new AtomicBoolean(false);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private final AtomicReference<ActionResultValue> result = new AtomicReference<>();

	private final boolean broadcast;
	private final boolean needsResult;
	private final String targetNode;

	protected AbstractEtcdActionValue(Instant attributionInstant, boolean broadcast, boolean needsResult,
			String targetNode) {
		super(attributionInstant);
		this.broadcast = broadcast;
		this.needsResult = needsResult;
		this.targetNode = targetNode;
	}

	protected AbstractEtcdActionValue(Instant attributionInstant, boolean broadcast, String targetNode) {
		this(attributionInstant, broadcast, !broadcast, targetNode);
	}

	protected AbstractEtcdActionValue(boolean broadcast, String targetNode) {
		this(new Date().toInstant(), broadcast, !broadcast, targetNode);
	}

	@Override
	public ActionResultValue execute(IEtcdClusterService clusterService) {

		this.setEtcdClusterService(clusterService);

		if (isDone() || isCancelled()) {
			return result.get();
		}

		ActionResultValue actionResult = doExecute();
		result.set(actionResult);
		done.set(true);
		return actionResult;
	}

	protected abstract ActionResultValue doExecute();

	@Override
	public boolean needsResult() {
		return needsResult;
	}

	@Override
	public boolean broadcast() {
		return broadcast;
	}

	@Override
	public String getTargetNode() {
		return targetNode;
	}

	@Override
	public boolean isDone() {
		return done.get();
	}

	@Override
	public boolean isCancelled() {
		return cancelled.get();
	}

	@Override
	public Serializable getResult() {
		return Optional.ofNullable(result.get()).map(ActionResultValue::getValue).orElse(null);
	}

	public void cancel() {
		cancelled.set(true);
	}

	private void setEtcdClusterService(IEtcdClusterService clusterService) {
		this.etcdClusterService = clusterService;
	}

}