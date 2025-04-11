package fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.etcd.cache;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.queuedtask.QueuedTaskEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdCachePanel extends Panel {

	private static final long serialVersionUID = -6235371376342468131L;
	
	@SpringBean
	private IEtcdClusterService etcdClusterService;

	private final IModel<List<QueuedTaskEtcdValue>> tasksModel;
	
	public ConsoleMaintenanceEtcdCachePanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		tasksModel = new LoadableDetachableModel<List<QueuedTaskEtcdValue>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected List<QueuedTaskEtcdValue> load() {
				return new ArrayList<>(etcdClusterService.getAllTasksFromCache().values());
			}
		};
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(tasksModel, Models
								.serializableModelFactory())
				)
						.addLabelColumn(
								new ResourceModel("business.etcd.queuedtask.taskid"),
								CoreWicketMoreBindings.queuedTaskEtcdValue()
										.taskId()
						)
						.addLabelColumn(
								new ResourceModel("business.etcd.queuedtask.node"),
								CoreWicketMoreBindings.queuedTaskEtcdValue()
										.nodeName()
						)
						.addLabelColumn(new ResourceModel("business.etcd.queuedtask.attribution"),
								CoreWicketMoreBindings.queuedTaskEtcdValue().attributionDate(),
								DatePattern.REALLY_SHORT_DATETIME)
						.bootstrapPanel()
						.title("console.maintenance.etcd.tasks")
						.responsive(Condition.alwaysTrue())
						.build("tasks")
		);
	}
	
	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(tasksModel);
	}
	

}
