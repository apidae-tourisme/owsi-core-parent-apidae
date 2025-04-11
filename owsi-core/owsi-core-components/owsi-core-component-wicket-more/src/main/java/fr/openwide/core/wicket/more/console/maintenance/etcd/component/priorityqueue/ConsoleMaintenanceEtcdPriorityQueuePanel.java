package fr.openwide.core.wicket.more.console.maintenance.etcd.component.priorityqueue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.priorityqueue.PriorityQueueEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdPriorityQueuePanel extends Panel {

	private static final long serialVersionUID = -6235371376342468131L;
	
	@SpringBean
	private IEtcdClusterService etcdClusterService;

	private final IModel<List<PriorityQueueEtcdValue>> priorityQueuesModel;
	
	public ConsoleMaintenanceEtcdPriorityQueuePanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		priorityQueuesModel = new LoadableDetachableModel<List<PriorityQueueEtcdValue>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected List<PriorityQueueEtcdValue> load() {
				return new ArrayList<>(etcdClusterService.getPriorityQueues().values());
			}
		};
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(priorityQueuesModel, Models
								.serializableModelFactory())
				)
						.addLabelColumn(
								new ResourceModel("business.etcd.priorityQueue.key"),
								CoreWicketMoreBindings.priorityQueueEtcdValue().priorityQueueKey()
						)
						.addLabelColumn(new ResourceModel("business.etcd.priorityQueue.nodename"),
								CoreWicketMoreBindings.priorityQueueEtcdValue().nodeName())
						.addLabelColumn(new ResourceModel("business.etcd.priorityQueue.attribution"),
								CoreWicketMoreBindings.priorityQueueEtcdValue().attributionDate(),
								DatePattern.REALLY_SHORT_DATETIME)
						.addLabelColumn(new ResourceModel("business.etcd.priorityQueue.attributions"),
								CoreWicketMoreBindings.priorityQueueEtcdValue().attributions())
						.bootstrapPanel()
						.title("console.maintenance.etcd.priorityQueues")
						.responsive(Condition.alwaysTrue())
						.build("priorityQueues")
		);
	}
	
	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(priorityQueuesModel);
	}
	

}
