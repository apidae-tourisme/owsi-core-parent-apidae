package fr.openwide.core.wicket.more.console.maintenance.queuemanager.component.etcd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;

import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderManager;
import fr.openwide.core.jpa.more.etcd.service.IEtcdQueueTaskManagerService;
import fr.openwide.core.jpa.more.infinispan.action.SwitchStatusQueueTaskManagerResult;
import fr.openwide.core.jpa.more.infinispan.model.QueueTaskManagerStatus;
import fr.openwide.core.jpa.more.infinispan.model.TaskQueueStatus;
import fr.openwide.core.wicket.markup.html.basic.CoreLabel;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.renderer.QueueManagerRenderer;
import fr.openwide.core.wicket.more.console.maintenance.queuemanager.renderer.QueueTaskRenderer;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.component.BootstrapLabel;
import fr.openwide.core.wicket.more.markup.html.factory.AbstractComponentFactory;
import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.markup.repeater.collection.CollectionView;
import fr.openwide.core.wicket.more.markup.repeater.table.DecoratedCoreDataTablePanel.AddInPlacement;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.markup.repeater.table.column.AbstractCoreColumn;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceQueueManagerNodeEtcdPanel extends Panel {

	private static final long serialVersionUID = -8384901751717369676L;

	public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMaintenanceQueueManagerNodeEtcdPanel.class);

	@SpringBean
	private IEtcdClusterService etcdClusterService;

	@SpringBean
	private IEtcdQueueTaskManagerService etcdQueueTaskManagerService;

	private final IModel<List<String>> nodesModel;

	public ConsoleMaintenanceQueueManagerNodeEtcdPanel(String id) {
		super(id);
		setOutputMarkupId(true);

		nodesModel = new LoadableDetachableModel<List<String>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected List<String> load() {
				return new ArrayList<>(etcdClusterService.getNodes().keySet());
			}
		};
		
		add(
				new CollectionView<String>("nodes", nodesModel, Models.<String>serializableModelFactory()) {
					private static final long serialVersionUID = 1L;
					@Override
					protected void populateItem(Item<String> item) {
						IModel<String> nodeModel = item.getModel();
						item.add(
								new NodeFragment("node", nodeModel)
						);
					}
				}
		);
	}

	private class NodeFragment extends Fragment {

		private static final long serialVersionUID = 1L;

		private final IModel<QueueTaskManagerStatus> queueTaskManagerStatusModel;

		private final IModel<Collection<TaskQueueStatus>> taskQueuesStatusModel;

		public NodeFragment(String id, IModel<String> nodeModel) {
			super(id, "node", ConsoleMaintenanceQueueManagerNodeEtcdPanel.this, nodeModel);
			setOutputMarkupId(true);

			queueTaskManagerStatusModel = new LoadableDetachableModel<QueueTaskManagerStatus>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected QueueTaskManagerStatus load() {
					return etcdQueueTaskManagerService.getQueueTaskManagerStatus(nodeModel.getObject());
				}
			};

			taskQueuesStatusModel = new LoadableDetachableModel<Collection<TaskQueueStatus>>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected Collection<TaskQueueStatus> load() {
					if (queueTaskManagerStatusModel.getObject() == null) {
						return null;
					}
					return queueTaskManagerStatusModel.getObject().getTaskQueueStatusById().values();
				}
			};

			add(
					DataTableBuilder.start(
							ReadOnlyCollectionModel.of(taskQueuesStatusModel, Models.serializableModelFactory())
					)
							.addLabelColumn(
									new ResourceModel("business.queuemanager.queueid"),
									CoreWicketMoreBindings.taskQueueStatus().id()
							)
							.addBootstrapLabelColumn(
									new ResourceModel("business.queuemanager.status"),
									Functions.identity(),
									QueueTaskRenderer.status()
							)
							.addColumn(
									new AbstractCoreColumn<TaskQueueStatus, ISort<?>>(new ResourceModel("business.queuemanager.thread")) {
										private static final long serialVersionUID = 1L;
										@Override
										public void populateItem(Item<ICellPopulator<TaskQueueStatus>> cellItem, String componentId, IModel<TaskQueueStatus> rowModel) {
											cellItem.add(
													new QueueThreadsFragment(componentId, "threads", rowModel.getObject().getNumberOfThreads())
											);
										}
									}
							)
							.addColumn(
									new AbstractCoreColumn<TaskQueueStatus, ISort<?>>(new ResourceModel("business.queuemanager.thread.running")) {
										private static final long serialVersionUID = 1L;
										@Override
										public void populateItem(Item<ICellPopulator<TaskQueueStatus>> cellItem, String componentId, IModel<TaskQueueStatus> rowModel) {
											cellItem.add(
													new QueueThreadsFragment(componentId, "threads", rowModel.getObject().getNumberOfRunningTasks())
											);
										}
									}
							)
							.addColumn(
									new AbstractCoreColumn<TaskQueueStatus, ISort<?>>(new ResourceModel("business.queuemanager.thread.waiting")) {
										private static final long serialVersionUID = 1L;
										@Override
										public void populateItem(Item<ICellPopulator<TaskQueueStatus>> cellItem, String componentId, IModel<TaskQueueStatus> rowModel) {
											cellItem.add(
													new QueueThreadsFragment(componentId, "threads", rowModel.getObject().getNumberOfWaitingTasks())
											);
										}
									}
							)
							.bootstrapPanel()
									.title(new AbstractComponentFactory<Component>() {
										private static final long serialVersionUID = 1L;
										@Override
										public Component create(String wicketId) {
											return new NodeTitleFragment(wicketId, nodeModel, queueTaskManagerStatusModel);
										}
									})
									.addIn(AddInPlacement.HEADING_RIGHT, new AbstractComponentFactory<NodeActionsFragment>() {
										private static final long serialVersionUID = 1L;
										@Override
										public NodeActionsFragment create(String wicketId) {
									return new NodeActionsFragment(wicketId, nodeModel, queueTaskManagerStatusModel);
										}
									})
									.responsive(Condition.alwaysTrue())
									.build("queueIds")
			);
		}

		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(queueTaskManagerStatusModel, taskQueuesStatusModel);
		}

	}

	private class NodeActionsFragment extends Fragment{

		private static final long serialVersionUID = 1L;

		private final IModel<Boolean> isActive;

		public NodeActionsFragment(String id, IModel<String> nodeModel,
				IModel<QueueTaskManagerStatus> queueTaskManagerStatusModel) {
			super(id, "nodeActions", ConsoleMaintenanceQueueManagerNodeEtcdPanel.this, nodeModel);
			setOutputMarkupId(true);

			isActive = new LoadableDetachableModel<Boolean>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected Boolean load() {
					if(queueTaskManagerStatusModel.getObject() == null){
						return null;
					}
					return queueTaskManagerStatusModel.getObject().isQueueManagerActive();
				}
			};

			add(
					new AjaxLink<String>("start", nodeModel) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onClick(AjaxRequestTarget target) {
							try {
								SwitchStatusQueueTaskManagerResult result = etcdQueueTaskManagerService
										.startQueueManager(nodeModel.getObject());
								if (SwitchStatusQueueTaskManagerResult.STARTED.equals(result)) {
									Session.get().success(getString("console.maintenance.queuemanager.actions.start.succes"));
								} else {
									Session.get().error(String.format("Erreur lors du démarrage du QueueTaskManager (%s)", result));
								}
								target.add(ConsoleMaintenanceQueueManagerNodeEtcdPanel.this);
							} catch (Exception e) {
								LOGGER.error("Erreur lors du démarrage du QueueTaskManager", e);
								Session.get().error(getString("common.error.unexpected"));
							}
							FeedbackUtils.refreshFeedback(target, getPage());
						}
					}
							.add(
									Condition.isFalse(isActive)
											.thenShow()
							),
					new AjaxLink<String>("stop", nodeModel) {
						private static final long serialVersionUID = 1L;
						@Override
						public void onClick(AjaxRequestTarget target) {
							try {
								SwitchStatusQueueTaskManagerResult result = etcdQueueTaskManagerService
										.stopQueueManager(nodeModel.getObject());
								if (SwitchStatusQueueTaskManagerResult.STOPPED.equals(result)) {
									Session.get().success(getString("console.maintenance.queuemanager.actions.stop.succes"));
								} else {
									Session.get().error(String.format("Erreur lors de l'arrêt du QueueTaskManager (%s)", result));
								}
								target.add(ConsoleMaintenanceQueueManagerNodeEtcdPanel.this);
							} catch (Exception e) {
								LOGGER.error("Erreur lors de l'arrêt du QueueTaskManager", e);
								Session.get().error(getString("common.error.unexpected"));
							}
							FeedbackUtils.refreshFeedback(target, getPage());
						}
					}
							.add(
									Condition.isTrue(isActive)
											.thenShow()
							)
			);
		}

		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(isActive);
		}

	}

	private class NodeTitleFragment extends Fragment{

		private static final long serialVersionUID = 1L;

		public NodeTitleFragment(String id, IModel<String> nodeModel,
				IModel<QueueTaskManagerStatus> queueTaskManagerStatusModel) {
			super(id, "nodeTitle", ConsoleMaintenanceQueueManagerNodeEtcdPanel.this);
			setOutputMarkupId(true);

			add(
					new CoreLabel("node", nodeModel),
					new BootstrapLabel<>("status", queueTaskManagerStatusModel, QueueManagerRenderer.status())
			);
		}

	}

	private class QueueThreadsFragment extends Fragment{

		private static final long serialVersionUID = 1L;

		@SpringBean
		private IQueuedTaskHolderManager queuedTaskHolderManager;

		private final IModel<Integer> nbThreadsModel;

		public QueueThreadsFragment(String id, String markupId, int nbThreads) {
			super(id, markupId, ConsoleMaintenanceQueueManagerNodeEtcdPanel.this);
			setOutputMarkupId(true);

			nbThreadsModel = new LoadableDetachableModel<Integer>(){
				private static final long serialVersionUID = 1L;
				@Override
				protected Integer load() {
					return nbThreads;
				}
			};

			add(
					new CoreLabel("thread", nbThreadsModel)
			);
		}

		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(nbThreadsModel);
		}

	}

	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(nodesModel);
	}

}
