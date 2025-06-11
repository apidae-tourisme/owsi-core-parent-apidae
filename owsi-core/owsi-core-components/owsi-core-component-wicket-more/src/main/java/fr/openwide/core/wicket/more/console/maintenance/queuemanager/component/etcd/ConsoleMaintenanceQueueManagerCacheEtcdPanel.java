package fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.etcd;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.commons.util.functional.Predicates2;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.util.model.Detachables;

public class ConsoleMaintenanceQueueManagerCacheEtcdPanel extends Panel {

	private static final long serialVersionUID = -8384901751717369676L;

	public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMaintenanceQueueManagerCacheEtcdPanel.class);

	@SpringBean
	private IEtcdQueueTaskManagerService etcdQueueTaskManagerService;

	private final IModel<Boolean> queueTaskManagerStatusModel;

	public ConsoleMaintenanceQueueManagerCacheEtcdPanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		queueTaskManagerStatusModel = new LoadableDetachableModel<Boolean>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected Boolean load() {
				return etcdQueueTaskManagerService.isOneQueueTaskManagerUp();
			}
		};
		
		add(
				new AjaxLink<Void>("emptyCache") {
					private static final long serialVersionUID = 1L;
					
					@Override
					public void onClick(AjaxRequestTarget target) {
						Integer nbTasksCleared = etcdQueueTaskManagerService.clearCache();
						getSession().success(new StringResourceModel("console.maintenance.queuemanager.actions.emptyCache.confirm")
								.setParameters(nbTasksCleared).getObject());
						FeedbackUtils.refreshFeedback(target, getPage());
					}
					
				}.add(Condition.predicate(queueTaskManagerStatusModel, Predicates2.isTrue()).thenDisable())
		);

	}
	
	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(queueTaskManagerStatusModel);
	}
}
