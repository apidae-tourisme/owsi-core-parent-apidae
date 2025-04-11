package fr.openwide.core.wicket.more.console.maintenance.etcd.component.nodes;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdNodesPanel extends Panel {

	private static final long serialVersionUID = 5155655164189659661L;
	
	@SpringBean
	private IEtcdClusterService etcdClusterService;
	
	private final IModel<List<NodeEtcdValue>> nodesModel;

	public ConsoleMaintenanceEtcdNodesPanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		nodesModel = new LoadableDetachableModel<List<NodeEtcdValue>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected List<NodeEtcdValue> load() {
				return new ArrayList<>(etcdClusterService.getNodes().values());
			}
		};
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(nodesModel, Models.serializableModelFactory())
				)
						.addLabelColumn(
								new ResourceModel("business.etcd.node.name"),
								CoreWicketMoreBindings.nodeEtcdValue().nodeName()
						)
						.addLabelColumn(
								new ResourceModel("business.etcd.node.attributionDate"),
								CoreWicketMoreBindings.nodeEtcdValue().attributionDate(),
								DatePattern.REALLY_SHORT_DATETIME
						)
						.bootstrapPanel()
								.title("console.maintenance.etcd.nodes")
								.responsive(Condition.alwaysTrue())
								.build("nodes")
		);
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(nodesModel);
	}

}
