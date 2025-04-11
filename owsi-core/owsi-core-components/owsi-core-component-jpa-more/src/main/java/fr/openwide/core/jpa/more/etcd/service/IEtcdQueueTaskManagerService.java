package fr.openwide.core.jpa.more.etcd.service;

import fr.openwide.core.jpa.more.infinispan.action.SwitchStatusQueueTaskManagerResult;
import fr.openwide.core.jpa.more.infinispan.model.QueueTaskManagerStatus;

public interface IEtcdQueueTaskManagerService {
	
	Boolean isOneQueueTaskManagerUp();

	QueueTaskManagerStatus getQueueTaskManagerStatus(String nodeName);

	QueueTaskManagerStatus createQueueTaskManagerStatus();

	SwitchStatusQueueTaskManagerResult start();

	SwitchStatusQueueTaskManagerResult stop();

	SwitchStatusQueueTaskManagerResult startQueueManager(String nodeName);

	SwitchStatusQueueTaskManagerResult stopQueueManager(String nodeName);
	
	Integer clearCache();

}
