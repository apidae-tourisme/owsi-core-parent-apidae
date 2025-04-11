package fr.openwide.core.jpa.more.etcd.action;

import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;

public class QueueTaskManagerStatusEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = -2019849149464876440L;

	@Autowired
	private transient IEtcdQueueTaskManagerService etcdQueueTaskManagerService;

	public QueueTaskManagerStatusEtcdAction(String target) {
		super(false, target);
	}

	@Override
	protected ActionResultValue doExecute() {
		return ActionResultValue.from(etcdQueueTaskManagerService.createQueueTaskManagerStatus());
	}

	public static final QueueTaskManagerStatusEtcdAction status(String target) {
		return new QueueTaskManagerStatusEtcdAction(target);
	}

}
