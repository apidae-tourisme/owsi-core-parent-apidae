package fr.openwide.core.wicket.more.console.maintenance.etcd.component.lock;

import java.util.Set;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.lock.model.EtcdLock;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdLocksPanel extends Panel {

	private static final long serialVersionUID = -6235371376342468131L;
	
	@SpringBean
	private IEtcdClusterService etcdClusterService;

	private final IModel<Set<EtcdLock>> locksModel;
	
	public ConsoleMaintenanceEtcdLocksPanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		locksModel = new LoadableDetachableModel<Set<EtcdLock>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected Set<EtcdLock> load() {
				return etcdClusterService.getLocks();
			}
		};
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(locksModel, Models.serializableModelFactory())
				)
						.addLabelColumn(
								new ResourceModel("business.etcd.lock.key"),
								CoreWicketMoreBindings.etcdLock().key()
						)
						.addLabelColumn(
								new ResourceModel("business.etcd.lock.fullKey"),
								CoreWicketMoreBindings.etcdLock().fullKey()
						)
						.addLabelColumn(new ResourceModel("business.etcd.lock.nodename"),
								CoreWicketMoreBindings.etcdLock().lockAttributionEtcdValue().nodeName())
						.addLabelColumn(new ResourceModel("business.etcd.lock.attribution"),
								CoreWicketMoreBindings.etcdLock().lockAttributionEtcdValue().attributionDate(),
								DatePattern.REALLY_SHORT_DATETIME)
						.addLabelColumn(new ResourceModel("business.etcd.lock.role"),
								CoreWicketMoreBindings.etcdLock().lockAttributionEtcdValue().role())
						.addLabelColumn(new ResourceModel("business.etcd.lock.priorityQueue"),
								CoreWicketMoreBindings.etcdLock().lockAttributionEtcdValue().priorityQueue())
						.bootstrapPanel()
						.title("console.maintenance.etcd.locks")
						.responsive(Condition.alwaysTrue())
						.build("locks")
		);
	}
	
	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(locksModel);
	}
	

}
