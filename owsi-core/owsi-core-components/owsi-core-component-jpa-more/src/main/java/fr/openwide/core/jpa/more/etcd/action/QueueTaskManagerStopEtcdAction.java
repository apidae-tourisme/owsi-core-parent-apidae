package fr.openwide.core.jpa.more.etcd.action;

import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;

public class QueueTaskManagerStopEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = -5968225724015355537L;

	@Autowired
	private transient IEtcdQueueTaskManagerService etcdQueueTaskManagerService;

	protected QueueTaskManagerStopEtcdAction(String target) {
		super(false, target);
	}

	@Override
	protected ActionResultValue doExecute() {
		return ActionResultValue.from(etcdQueueTaskManagerService.stop());
	}

	public static final QueueTaskManagerStopEtcdAction stop(String target) {
		return new QueueTaskManagerStopEtcdAction(target);
	}

}
