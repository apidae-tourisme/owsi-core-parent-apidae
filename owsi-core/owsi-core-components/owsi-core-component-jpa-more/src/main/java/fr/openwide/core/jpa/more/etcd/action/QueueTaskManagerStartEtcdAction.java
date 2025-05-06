package fr.openwide.core.jpa.more.etcd.action;

import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;

public class QueueTaskManagerStartEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = 1854734111545990917L;

	@Autowired
	private transient IEtcdQueueTaskManagerService etcdQueueTaskManagerService;

	protected QueueTaskManagerStartEtcdAction(String target) {
		super(false, target);
	}

	@Override
	protected ActionResultValue doExecute() {
		return ActionResultValue.from(etcdQueueTaskManagerService.start());
	}

	public static final QueueTaskManagerStartEtcdAction start(String address) {
		return new QueueTaskManagerStartEtcdAction(address);
	}

}
