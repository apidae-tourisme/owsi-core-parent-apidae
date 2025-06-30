package fr.openwide.core.wicket.more.console.maintenance.etcd.component.cluster;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.wicket.markup.html.basic.CoreLabel;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.console.maintenance.etcd.renderer.NodeEtcdRenderer;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.component.BootstrapBadge;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.markup.repeater.table.column.AbstractCoreColumn;
import fr.openwide.core.wicket.more.model.BindingModel;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdClusterPanel extends Panel {

	private static final long serialVersionUID = -3170379589959735719L;

	@SpringBean
	private IEtcdClusterService etcdClusterService;

	private final IModel<List<NodeEtcdValue>> nodesModel;

	public ConsoleMaintenanceEtcdClusterPanel(String id) {
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
						.addColumn(
								new AbstractCoreColumn<NodeEtcdValue, ISort<?>>(
										new ResourceModel("business.etcd.node.name")) {
									private static final long serialVersionUID = 1L;
									@Override
									public void populateItem(Item<ICellPopulator<NodeEtcdValue>> cellItem,
											String componentId, IModel<NodeEtcdValue> rowModel) {
										cellItem.add(
												new NodeAddressFragment(componentId, rowModel)
										);
									}
								}
						)
						.addLabelColumn(
								new ResourceModel("business.etcd.node.attributionDate"),
								CoreWicketMoreBindings.nodeEtcdValue().attributionDate(),
								DatePattern.REALLY_SHORT_DATETIME
						)
						.bootstrapPanel()
						.title("console.maintenance.etcd.cluster")
								.responsive(Condition.alwaysTrue())
								.build("cluster")
		);
	}

	private class NodeAddressFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		public NodeAddressFragment(String id, IModel<NodeEtcdValue> nodeModel) {
			super(id, "nodeAddress", ConsoleMaintenanceEtcdClusterPanel.this, nodeModel);
			
			add(
					new CoreLabel("name",
							BindingModel.of(nodeModel, CoreWicketMoreBindings.nodeEtcdValue().nodeName())),
					new BootstrapBadge<>("local", BindingModel.of(nodeModel, CoreWicketMoreBindings.nodeEtcdValue()),
							NodeEtcdRenderer.local())
			);
		}
		
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(nodesModel);
	}

}
