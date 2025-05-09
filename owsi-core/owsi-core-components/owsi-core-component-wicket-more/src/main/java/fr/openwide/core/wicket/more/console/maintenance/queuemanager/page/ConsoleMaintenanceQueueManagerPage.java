package fr.openwide.core.wicket.more.console.maintenance.queuemanager.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import fr.openwide.core.jpa.more.property.JpaMoreEtcdPropertyIds;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.etcd.ConsoleMaintenanceQueueManagerCacheEtcdPanel;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.etcd.ConsoleMaintenanceQueueManagerNodeEtcdPanel;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.infinispan.ConsoleMaintenanceQueueManagerCacheInfinispanPanel;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.infinispan.ConsoleMaintenanceQueueManagerNodePanel;
import fr.openwide.core.wicket.more.console.maintenance.template.ConsoleMaintenanceTemplate;
import fr.openwide.core.wicket.more.console.template.ConsoleTemplate;

public class ConsoleMaintenanceQueueManagerPage extends ConsoleMaintenanceTemplate {

	private static final long serialVersionUID = 4288903243206618631L;

	public ConsoleMaintenanceQueueManagerPage(PageParameters parameters) {
		super(parameters);
		if (Boolean.TRUE.equals(propertyService.get(JpaMoreEtcdPropertyIds.ETCD_ENABLED))) {
			add(new ConsoleMaintenanceQueueManagerCacheEtcdPanel("cacheInfinispan"),
					new ConsoleMaintenanceQueueManagerNodeEtcdPanel("nodes"));
		} else {
			add(new ConsoleMaintenanceQueueManagerCacheInfinispanPanel("cacheInfinispan"),
					new ConsoleMaintenanceQueueManagerNodePanel("nodes"));
		}
	}

	@Override
	protected Class<? extends ConsoleTemplate> getMenuItemPageClass() {
		return null;
	}

}
