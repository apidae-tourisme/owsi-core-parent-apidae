package fr.openwide.core.wicket.more.console.maintenance.task.component;

import java.util.Collection;
import java.util.Date;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;

import fr.openwide.core.jpa.more.business.task.util.TaskResult;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;
import fr.openwide.core.wicket.markup.html.basic.CountLabel;
import fr.openwide.core.wicket.more.console.maintenance.task.model.QueuedTaskHolderDataProvider;
import fr.openwide.core.wicket.more.markup.html.form.DatePicker;
import fr.openwide.core.wicket.more.markup.html.form.LabelPlaceholderBehavior;
import fr.openwide.core.wicket.more.util.DatePattern;

public class TaskFilterPanel extends Panel {

	private static final long serialVersionUID = -3803340118726908397L;

	private final IPageable pageable;

	public TaskFilterPanel(String id, final QueuedTaskHolderDataProvider queuedTaskHolderDataProvider,
			IPageable pageable) {
		super(id);
		this.pageable = pageable;

		final IModel<Long> rowCountModel = new LoadableDetachableModel<Long>() {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected Long load() {
				return queuedTaskHolderDataProvider.size();
			}
		};

		add(new CountLabel("topCount", "console.maintenance.task.common.count", rowCountModel) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(queuedTaskHolderDataProvider.size() > 0);
			}
		});

		Form<Void> filterForm = new Form<Void>("filterForm") {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onSubmit() {
				super.onSubmit();
				TaskFilterPanel.this.pageable.setCurrentPage(0);
			}
		};
		add(filterForm);

		FormComponent<String> name = new TextField<String>("name", queuedTaskHolderDataProvider.getNameModel());
		name.setLabel(new ResourceModel("console.maintenance.task.common.name"));
		name.add(new LabelPlaceholderBehavior());
		filterForm.add(name);

		FormComponent<Collection<String>> taskTypes = new TaskTypeListMultipleChoice("taskTypes",
				queuedTaskHolderDataProvider.getTaskTypesModel());
		filterForm.add(taskTypes);

		FormComponent<Collection<String>> queueIds = new TaskQueueIdListMultipleChoice("queueIds",
				queuedTaskHolderDataProvider.getQueueIdsModel());
		filterForm.add(queueIds);

		FormComponent<Collection<TaskStatus>> statuses = new TaskStatusListMultipleChoice("statuses",
				queuedTaskHolderDataProvider.getStatusesModel());
		filterForm.add(statuses);

		FormComponent<Collection<TaskResult>> results = new TaskResultListMultipleChoice("results",
				queuedTaskHolderDataProvider.getResultsModel());
		filterForm.add(results);
		
		FormComponent<Date> date = new DatePicker("date", queuedTaskHolderDataProvider.getCreationDateModel(), DatePattern.SHORT_DATE);
		date.setRequired(true);
		filterForm.add(date);
	}
}
