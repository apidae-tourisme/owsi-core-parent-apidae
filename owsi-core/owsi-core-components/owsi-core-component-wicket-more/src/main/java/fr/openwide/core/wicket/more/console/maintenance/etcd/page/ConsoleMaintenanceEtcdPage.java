package fr.openwide.core.wicket.more.console.maintenance.etcd.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.collect.ImmutableSet;

import fr.openwide.core.jpa.more.property.JpaMoreEtcdPropertyIds;
import fr.openwide.core.spring.property.model.PropertyId;
import fr.openwide.core.wicket.more.console.common.component.PropertyIdListPanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.cluster.ConsoleMaintenanceEtcdClusterPanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.lock.ConsoleMaintenanceEtcdLocksPanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.nodes.ConsoleMaintenanceEtcdNodesPanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.priorityqueue.ConsoleMaintenanceEtcdPriorityQueuePanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.roles.ConsoleMaintenanceEtcdRolesPanel;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.rolesrequests.ConsoleMaintenanceEtcdRolesRequestsPanel;
import fr.openwide.core.wicket.more.console.maintenance.template.ConsoleMaintenanceTemplate;
import fr.openwide.core.wicket.more.console.template.ConsoleTemplate;

public class ConsoleMaintenanceEtcdPage extends ConsoleMaintenanceTemplate {

	private static final long serialVersionUID = 2373051508004389589L;

	public ConsoleMaintenanceEtcdPage(PageParameters parameters) {
		super(parameters);

		add(new ConsoleMaintenanceEtcdClusterPanel("cluster"),
				new ConsoleMaintenanceEtcdRolesPanel("roles"),
				new ConsoleMaintenanceEtcdLocksPanel("locks"),
				new ConsoleMaintenanceEtcdNodesPanel("nodes"),
				new ConsoleMaintenanceEtcdRolesRequestsPanel("rolesRequests"),
				new ConsoleMaintenanceEtcdPriorityQueuePanel("priorityQueues"),
				new PropertyIdListPanel("propertyIds",
						ImmutableSet.<PropertyId<?>>builder()
								.add(JpaMoreEtcdPropertyIds.ETCD_ENABLED,
										JpaMoreEtcdPropertyIds.ETCD_FLUSH_STATISTIC_ENABLED,
										JpaMoreEtcdPropertyIds.ETCD_NODE_NAME,
										JpaMoreEtcdPropertyIds.ETCD_ENDPOINTS, 
										JpaMoreEtcdPropertyIds.ETCD_ROLES,
										JpaMoreEtcdPropertyIds.ETCD_DEFAULT_LEASE_TTL)
								.build()));
	}

	@Override
	protected Class<? extends ConsoleTemplate> getMenuItemPageClass() {
		return null;
	}

}
