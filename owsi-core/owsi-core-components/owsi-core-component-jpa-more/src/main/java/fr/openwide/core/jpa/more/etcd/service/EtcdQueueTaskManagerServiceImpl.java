package fr.openwide.core.jpa.more.etcd.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.jpa.more.business.task.model.IQueueId;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderManager;
import fr.openwide.core.jpa.more.etcd.action.QueueTaskManagerStartEtcdAction;
import fr.openwide.core.jpa.more.etcd.action.QueueTaskManagerStatusEtcdAction;
import fr.openwide.core.jpa.more.etcd.action.QueueTaskManagerStopEtcdAction;
import fr.openwide.core.jpa.more.infinispan.action.SwitchStatusQueueTaskManagerResult;
import fr.openwide.core.jpa.more.infinispan.model.QueueTaskManagerStatus;
import fr.openwide.core.jpa.more.infinispan.model.TaskQueueStatus;

public class EtcdQueueTaskManagerServiceImpl implements IEtcdQueueTaskManagerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtcdQueueTaskManagerServiceImpl.class);

	@Autowired(required = false)
	private IEtcdClusterService etcdClusterService;

	@Autowired
	private IQueuedTaskHolderManager queuedTaskHolderManager;

	@Override
	public QueueTaskManagerStatus getQueueTaskManagerStatus(String nodeName) {
		try {
			return etcdClusterService.syncedAction(QueueTaskManagerStatusEtcdAction.status(nodeName), 10,
					TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			LOGGER.error("Erreur lors de la récupération du QueueTaskManagerStatus", e);
		} catch (TimeoutException e) {
			LOGGER.error("Timeout lors de la récupération du QueueTaskManagerStatus");
		}
		return null;
	}

	@Override
	public QueueTaskManagerStatus createQueueTaskManagerStatus() {
		QueueTaskManagerStatus queueTaskManagerStatus = new QueueTaskManagerStatus();

		queueTaskManagerStatus.setQueueManagerActive(queuedTaskHolderManager.isActive());

		Map<String, IQueueId> queuesById = new HashMap<>();
		Map<String, TaskQueueStatus> taskQueueStatusById = Maps.newHashMap();

		for (IQueueId queueId : queuedTaskHolderManager.getQueueIds()) {
			String id = queueId.getUniqueStringId();
			queuesById.put(id, queueId);
			taskQueueStatusById.put(id,
					new TaskQueueStatus(queueId.getUniqueStringId(), queuedTaskHolderManager.isTaskQueueActive(id),
							queuedTaskHolderManager.getNumberOfRunningTasks(id),
							queuedTaskHolderManager.getNumberOfWaitingTasks(id),
							queuedTaskHolderManager.getNumberOfTaskConsumer(id)));
		}
		queueTaskManagerStatus.setQueuesById(queuesById);
		queueTaskManagerStatus.setTaskQueueStatusById(taskQueueStatusById);

		return queueTaskManagerStatus;
	}

	@Override
	public SwitchStatusQueueTaskManagerResult startQueueManager(String nodeName) {
		try {
			return etcdClusterService.syncedAction(QueueTaskManagerStartEtcdAction.start(nodeName), 10,
					TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			LOGGER.error("Erreur lors de la récupération du QueueTaskManagerStatus", e);
		} catch (TimeoutException e) {
			LOGGER.error("Timeout lors de la récupération du QueueTaskManagerStatus");
			return SwitchStatusQueueTaskManagerResult.TIME_OUT;
		}
		return null;
	}

	@Override
	public SwitchStatusQueueTaskManagerResult stopQueueManager(String nodeName) {
		try {
			return etcdClusterService.syncedAction(QueueTaskManagerStopEtcdAction.stop(nodeName), 10,
					TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			LOGGER.error("Erreur lors de la récupération du QueueTaskManagerStatus", e);
		} catch (TimeoutException e) {
			LOGGER.error("Timeout lors de la récupération du QueueTaskManagerStatus");
			return SwitchStatusQueueTaskManagerResult.TIME_OUT;
		}
		return null;
	}

	@Override
	public SwitchStatusQueueTaskManagerResult start() {
		if (queuedTaskHolderManager.isActive()) {
			return SwitchStatusQueueTaskManagerResult.ALREADY_STARTED;
		}
		queuedTaskHolderManager.start();
		return SwitchStatusQueueTaskManagerResult.STARTED;
	}

	@Override
	public SwitchStatusQueueTaskManagerResult stop() {
		if (!queuedTaskHolderManager.isActive()) {
			return SwitchStatusQueueTaskManagerResult.ALREADY_STOPPED;
		}
		queuedTaskHolderManager.stop();
		return SwitchStatusQueueTaskManagerResult.STOPPED;
	}

	@Override
	public Boolean isOneQueueTaskManagerUp() {
		return etcdClusterService.getNodes().keySet().stream()
				.map(this::getQueueTaskManagerStatus)
				.filter(Objects::nonNull)
				.anyMatch(QueueTaskManagerStatus::isQueueManagerActive);
	}

	@Override
	public Integer clearCache() {
		return queuedTaskHolderManager.clearCache();
	}

}
