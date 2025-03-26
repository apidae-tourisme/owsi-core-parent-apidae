package fr.openwide.core.jpa.more.business.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.jpa.business.generic.service.GenericEntityServiceImpl;
import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.more.business.task.dao.IQueuedTaskHolderDao;
import fr.openwide.core.jpa.more.business.task.model.QueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.model.TaskTypesRegistry;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;

public class QueuedTaskHolderServiceImpl extends GenericEntityServiceImpl<Long, QueuedTaskHolder> implements
		IQueuedTaskHolderService {
	
	private IQueuedTaskHolderDao queuedTaskHolderDao;
	private final TaskTypesRegistry typesRegistry;

	@Autowired
	public QueuedTaskHolderServiceImpl(IQueuedTaskHolderDao queuedTaskHolderDao, TaskTypesRegistry typesRegistry) {
		super(queuedTaskHolderDao);
		this.queuedTaskHolderDao = queuedTaskHolderDao;
		this.typesRegistry = typesRegistry;
	}

	@Override
	protected void createEntity(QueuedTaskHolder queuedTaskHolder) throws ServiceException, SecurityServiceException {
		queuedTaskHolder.setCreationDate(new Date());
		super.createEntity(queuedTaskHolder);
	}

	@Override
	public Long count(Date since, TaskStatus... statuses) {
		return queuedTaskHolderDao.count(since, statuses);
	}
	
	@Override
	public Long count(TaskStatus... statuses) {
		return queuedTaskHolderDao.count(statuses);
	}

	@Override
	public QueuedTaskHolder getNextTaskForExecution(String taskType) {
		return queuedTaskHolderDao.getNextTaskForExecution(taskType);
	}

	@Override
	public QueuedTaskHolder getRandomStalledTask(String taskType, int executionTimeLimitInSeconds) {
		return queuedTaskHolderDao.getStalledTask(taskType, executionTimeLimitInSeconds);
	}
	
	@Override
	public List<QueuedTaskHolder> getListConsumable(String queueId, Integer limit)  throws ServiceException, SecurityServiceException {
		return queuedTaskHolderDao.listConsumable(queueId, limit);
	}

	/**
	 * Task types extracted from {@link TaskTypesRegistry}, sorted in alphabetical order.
	 */
	@Override
	public List<String> listTypes() {
		return new ArrayList<>(typesRegistry.getTypes());
	}
	
	@Override
	public boolean isReloadable(QueuedTaskHolder task) {
		return TaskStatus.RELOADABLE_TASK_STATUS.contains(task.getStatus());
	}

	@Override
	public boolean isCancellable(QueuedTaskHolder task) {
		return TaskStatus.CANCELLABLE_TASK_STATUS.contains(task.getStatus());
	}
}
