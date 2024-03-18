package fr.openwide.core.wicket.more.console.maintenance.task.model;

import java.util.Collection;
import java.util.Date;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.CollectionModel;

import fr.openwide.core.jpa.more.business.search.query.ISearchQuery;
import fr.openwide.core.jpa.more.business.task.model.QueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.search.IQueuedTaskHolderSearchQuery;
import fr.openwide.core.jpa.more.business.task.search.QueuedTaskHolderSort;
import fr.openwide.core.jpa.more.business.task.util.TaskResult;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;
import fr.openwide.core.wicket.more.markup.html.sort.model.CompositeSortModel;
import fr.openwide.core.wicket.more.markup.html.sort.model.CompositeSortModel.CompositingStrategy;
import fr.openwide.core.wicket.more.model.AbstractSearchQueryDataProvider;
import fr.openwide.core.wicket.more.model.GenericEntityModel;

public class QueuedTaskHolderDataProvider extends AbstractSearchQueryDataProvider<QueuedTaskHolder, QueuedTaskHolderSort> {

	private static final long serialVersionUID = -1886156254057416250L;

	private final IModel<String> nameModel = new Model<String>();

	private final IModel<Collection<TaskStatus>> statusesModel = new CollectionModel<TaskStatus>();

	private final IModel<Collection<TaskResult>> resultsModel = new CollectionModel<TaskResult>();

	private final IModel<Collection<String>> taskTypesModel = new CollectionModel<String>();

	private final IModel<Collection<String>> queueIdsModel = new CollectionModel<String>();

	private final IModel<Date> creationDateModel = new Model<Date>();

	private final IModel<Date> startDateModel = new Model<Date>();

	private final IModel<Date> endDateModel = new Model<Date>();
	
	private final CompositeSortModel<QueuedTaskHolderSort> sortModel = new CompositeSortModel<>(
			CompositingStrategy.LAST_ONLY,
			QueuedTaskHolderSort.CREATION_DATE
	);

	public QueuedTaskHolderDataProvider() {
		super();
		creationDateModel.setObject(new Date());
		Injector.get().inject(this);
	}

	public IModel<String> getNameModel() {
		return nameModel;
	}

	public IModel<Collection<TaskStatus>> getStatusesModel() {
		return statusesModel;
	}

	public IModel<Collection<TaskResult>> getResultsModel() {
		return resultsModel;
	}

	public IModel<Collection<String>> getTaskTypesModel() {
		return taskTypesModel;
	}
	
	public IModel<Collection<String>> getQueueIdsModel() {
		return queueIdsModel;
	}

	public IModel<Date> getCreationDateModel() {
		return creationDateModel;
	}

	public IModel<Date> getStartDateModel() {
		return startDateModel;
	}

	public IModel<Date> getCompletionDateModel() {
		return endDateModel;
	}

	@Override
	public IModel<QueuedTaskHolder> model(QueuedTaskHolder object) {
		return new GenericEntityModel<Long, QueuedTaskHolder>(object);
	}

	@Override
	public void detach() {
		super.detach();
		
		nameModel.detach();
		statusesModel.detach();
		resultsModel.detach();
		taskTypesModel.detach();
		queueIdsModel.detach();
		creationDateModel.detach();
		startDateModel.detach();
		endDateModel.detach();
	}

	@Override
	protected ISearchQuery<QueuedTaskHolder, QueuedTaskHolderSort> getSearchQuery() {
		return createSearchQuery(IQueuedTaskHolderSearchQuery.class)
				.name(nameModel.getObject())
				.statuses(statusesModel.getObject())
				.results(resultsModel.getObject())
				.types(taskTypesModel.getObject())
				.queueIds(queueIdsModel.getObject())
				.creationDate(creationDateModel.getObject())
				.startDate(startDateModel.getObject())
				.endDate(endDateModel.getObject())
				.sort(sortModel.getObject());
	}
}
