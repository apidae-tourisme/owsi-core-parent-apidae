package fr.openwide.core.wicket.more.console.maintenance.etcd.component.rolesrequests;

import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import fr.openwide.core.commons.util.functional.SerializablePredicate;
import fr.openwide.core.etcd.cache.model.rolerequest.RoleRequestEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.wicket.markup.html.basic.CoreLabel;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.markup.html.action.AbstractOneParameterAjaxAction;
import fr.openwide.core.wicket.more.markup.html.basic.DateLabel;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.model.BootstrapColor;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRenderer;
import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.markup.repeater.table.builder.DataTableBuilder;
import fr.openwide.core.wicket.more.markup.repeater.table.column.AbstractCoreColumn;
import fr.openwide.core.wicket.more.model.BindingModel;
import fr.openwide.core.wicket.more.model.ReadOnlyCollectionModel;
import fr.openwide.core.wicket.more.util.DatePattern;
import fr.openwide.core.wicket.more.util.binding.CoreWicketMoreBindings;
import fr.openwide.core.wicket.more.util.model.Detachables;
import fr.openwide.core.wicket.more.util.model.Models;

public class ConsoleMaintenanceEtcdRolesRequestsPanel extends Panel {

	private static final long serialVersionUID = 2410247581542059920L;

	public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMaintenanceEtcdRolesRequestsPanel.class);

	@SpringBean
	private IEtcdClusterService etcdClusterService;
	
	private final IModel<Set<IRole>> rolesModel;

	public ConsoleMaintenanceEtcdRolesRequestsPanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		rolesModel = new LoadableDetachableModel<Set<IRole>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected Set<IRole> load() {
				return etcdClusterService.getAllRolesForRolesRequests();
			}
			
		};
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(rolesModel, Models.serializableModelFactory())
				)
						.addColumn(
								new AbstractCoreColumn<IRole, ISort<?>>(
										new ResourceModel("business.etcd.role.request.key")) {
									private static final long serialVersionUID = 1L;
									@Override
									public void populateItem(Item<ICellPopulator<IRole>> cellItem, String componentId, IModel<IRole> rowModel) {
										cellItem.add(
												new RoleKeyFragment(componentId, rowModel)
										);
									}
								}
						)
						.addColumn(
								new AbstractCoreColumn<IRole, ISort<?>>(
										new ResourceModel("business.etcd.role.request.address")) {
									private static final long serialVersionUID = 1L;
									@Override
									public void populateItem(Item<ICellPopulator<IRole>> cellItem, String componentId, IModel<IRole> rowModel) {
										cellItem.add(
												new RoleAddressFragment(componentId, rowModel)
										);
									}
								}
						)
						.addColumn(
								new AbstractCoreColumn<IRole, ISort<?>>(
										new ResourceModel("business.etcd.role.request.attributionDate")) {
									private static final long serialVersionUID = 1L;
									@Override
									public void populateItem(Item<ICellPopulator<IRole>> cellItem, String componentId, IModel<IRole> rowModel) {
										cellItem.add(
												new RoleAttributionDateFragment(componentId, rowModel)
										);
									}
								}
						)
						.addActionColumn()
								.addConfirmAction(
								BootstrapRenderer.constant("console.maintenance.etcd.roles.requests.actions.delete",
										"fa fa-times fa-fw", BootstrapColor.DANGER)
								)
						.title(new ResourceModel(
								"console.maintenance.etcd.roles.requests.actions.delete.confirm.title"))
						.content(new ResourceModel(
								"console.maintenance.etcd.roles.requests.actions.delete.confirm.content"))
										.yesNo()
										.onClick(
												new AbstractOneParameterAjaxAction<IModel<IRole>>() {
													private static final long serialVersionUID = 1L;
													@Override
													public void execute(AjaxRequestTarget target, IModel<IRole> model) {
														try {
															etcdClusterService.removeRoleRequest(model.getObject());
											Session.get().success(getString(
													"console.maintenance.etcd.roles.requests.actions.delete.success"));
															target.add(ConsoleMaintenanceEtcdRolesRequestsPanel.this);
														} catch (Exception e) {
															LOGGER.error("Erreur lors de la suppression d'un rôle.");
															Session.get().error(getString("common.error.unexpected"));
														}
														FeedbackUtils.refreshFeedback(target, getPage());
													}
												}
										)
										.when(
												new SerializablePredicate<IRole>() {
													private static final long serialVersionUID = 1L;
													public boolean apply(IRole input) {
														RoleRequestEtcdValue roleAttribution = etcdClusterService.getRoleRequest(input);
										return roleAttribution != null && roleAttribution.getAttributionDate() != null
												&& roleAttribution.getNodeName() != null;
													}
												}
										)
										.withClassOnElements("btn-xs")
								.end()
						.bootstrapPanel()
								.title("console.maintenance.etcd.roles.requests")
								.responsive(Condition.alwaysTrue())
								.build("roles")
		);
	}
	
	private class RoleKeyFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		public RoleKeyFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleKey", ConsoleMaintenanceEtcdRolesRequestsPanel.this, roleModel);
			
			add(
					new CoreLabel("key", BindingModel.of(roleModel, CoreWicketMoreBindings.iRole().key()))
							.add(
									new AttributeModifier("title", BindingModel.of(roleModel, CoreWicketMoreBindings.iRole().key()))
							)
			);
		}
		
	}
	
	private class RoleAddressFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		@SpringBean
		private IEtcdClusterService etcdClusterService;
		
		private final IModel<RoleRequestEtcdValue> attributionModel;
		
		public RoleAddressFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleAddress", ConsoleMaintenanceEtcdRolesRequestsPanel.this, roleModel);
			
			attributionModel = new LoadableDetachableModel<RoleRequestEtcdValue>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected RoleRequestEtcdValue load() {
					return etcdClusterService.getRoleRequest(roleModel.getObject());
				}
				
			};
			
			add(
					new CoreLabel("address", BindingModel.of(attributionModel,
							CoreWicketMoreBindings.rodeRequestEtcdValue().nodeName()))
			);
		}
		
		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(attributionModel);
		}
	}

	private class RoleAttributionDateFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		@SpringBean
		private IEtcdClusterService etcdClusterService;
		
		private final IModel<RoleRequestEtcdValue> attributionModel;
		
		public RoleAttributionDateFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleAttributionDate", ConsoleMaintenanceEtcdRolesRequestsPanel.this, roleModel);
			
			attributionModel = new LoadableDetachableModel<RoleRequestEtcdValue>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected RoleRequestEtcdValue load() {
					return etcdClusterService.getRoleRequest(roleModel.getObject());
				}
				
			};
			
			add(
					new DateLabel("attributionDate",
							BindingModel.of(attributionModel,
									CoreWicketMoreBindings.rodeRequestEtcdValue().attributionDate()),
							DatePattern.REALLY_SHORT_DATETIME)
			);
		}
		
		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(attributionModel);
		}
	}
	
	@Override
	protected void onDetach(){
		super.onDetach();
		Detachables.detach(rolesModel);
	}

}
