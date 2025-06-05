package fr.openwide.core.etcd.cache.model.queuedtask;

import java.time.Instant;
import java.util.Date;

import org.bindgen.Bindable;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

@Bindable
public class QueuedTaskEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -5549707966478459308L;

	private String nodeName;

	private String taskId;

	private QueuedTaskEtcdValue(Instant attributionInstant, String nodeName, String taskId) {
		super(attributionInstant);
		this.nodeName = nodeName;
		this.taskId = taskId;
	}

	public static final QueuedTaskEtcdValue from(Date attributionDate, String nodeName, String taskId) {
		return new QueuedTaskEtcdValue(attributionDate.toInstant(), nodeName, taskId);

	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

}
