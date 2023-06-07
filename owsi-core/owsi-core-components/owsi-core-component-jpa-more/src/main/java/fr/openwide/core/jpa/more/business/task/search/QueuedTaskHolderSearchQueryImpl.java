package fr.openwide.core.jpa.more.business.task.search;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

import fr.openwide.core.jpa.more.business.search.query.AbstractJpaSearchQuery;
import fr.openwide.core.jpa.more.business.task.model.QQueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.model.QueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.util.TaskResult;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;

public class QueuedTaskHolderSearchQueryImpl extends AbstractJpaSearchQuery<QueuedTaskHolder, QueuedTaskHolderSort>
		implements IQueuedTaskHolderSearchQuery {

	public QueuedTaskHolderSearchQueryImpl() {
		super(QQueuedTaskHolder.queuedTaskHolder);
	}

	@Override
	public IQueuedTaskHolderSearchQuery name(String name) {
		must(matchIfGiven(QQueuedTaskHolder.queuedTaskHolder.name, name));
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery statuses(Collection<TaskStatus> statuses) {
		must(matchOneIfGiven(QQueuedTaskHolder.queuedTaskHolder.status, statuses));
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery results(Collection<TaskResult> results) {
		must(matchOneIfGiven(QQueuedTaskHolder.queuedTaskHolder.result, results));
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery types(Collection<String> types) {
		must(matchOneIfGiven(QQueuedTaskHolder.queuedTaskHolder.taskType, types));
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery queueIds(Collection<String> queueIds) {
		must(matchOneIfGiven(QQueuedTaskHolder.queuedTaskHolder.queueId, queueIds));
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery creationDate(Date creationDate) {
		if (creationDate != null) {
			Date start = DateUtils.truncate(creationDate, Calendar.DATE);
			Date end = DateUtils.addDays(DateUtils.truncate(creationDate, Calendar.DATE), 1);
			must(QQueuedTaskHolder.queuedTaskHolder.creationDate.between(start, end));
		}
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery startDate(Date startDate) {
		if (startDate != null) {
			must(QQueuedTaskHolder.queuedTaskHolder.startDate.loe(startDate));
		}
		return this;
	}

	@Override
	public IQueuedTaskHolderSearchQuery endDate(Date endDate) {
		if (endDate != null) {
			must(QQueuedTaskHolder.queuedTaskHolder.endDate.loe(endDate));
		}
		return this;
	}

}
