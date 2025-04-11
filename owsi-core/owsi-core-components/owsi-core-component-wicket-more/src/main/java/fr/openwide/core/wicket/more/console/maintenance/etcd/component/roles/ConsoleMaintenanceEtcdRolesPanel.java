package fr.openwide.core.wicket.more.console.maintenance.etcd.component.roles;

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
import fr.openwide.core.etcd.cache.model.role.RoleEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.jpa.more.business.sort.ISort;
import fr.openwide.core.wicket.markup.html.basic.CoreLabel;
import fr.openwide.core.wicket.more.condition.Condition;
import fr.openwide.core.wicket.more.console.maintenance.etcd.form.ConsoleMaintenanceEtcdRoleAssignPopup;
import fr.openwide.core.wicket.more.markup.html.action.AbstractOneParameterAjaxAction;
import fr.openwide.core.wicket.more.markup.html.action.OneParameterModalOpenAjaxAction;
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

public class ConsoleMaintenanceEtcdRolesPanel extends Panel {

	private static final long serialVersionUID = -6133208190633994682L;

	public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMaintenanceEtcdRolesPanel.class);

	@SpringBean
	private IEtcdClusterService etcdClusterService;
	
	private final IModel<Set<IRole>> rolesModel;

	public ConsoleMaintenanceEtcdRolesPanel(String id) {
		super(id);
		setOutputMarkupId(true);
		
		rolesModel = new LoadableDetachableModel<Set<IRole>>() {
			private static final long serialVersionUID = 1L;
			@Override
			protected Set<IRole> load() {
				return etcdClusterService.getAllRolesForAssignation();
			}
		};
		
		ConsoleMaintenanceEtcdRoleAssignPopup assignPopup = new ConsoleMaintenanceEtcdRoleAssignPopup(
				"assignPopup");
		add(assignPopup);
		
		add(
				DataTableBuilder.start(
						ReadOnlyCollectionModel.of(rolesModel, Models.serializableModelFactory())
				)
						.addColumn(
								new AbstractCoreColumn<IRole, ISort<?>>(new ResourceModel("business.etcd.role.key")) {
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
										new ResourceModel("business.etcd.role.address")) {
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
										new ResourceModel("business.etcd.role.attributionDate")) {
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
						.addAction(
								BootstrapRenderer.constant("console.maintenance.infinispan.roles.actions.assign",
										"fa fa-exchange fa-fw", BootstrapColor.PRIMARY),
								new OneParameterModalOpenAjaxAction<IModel<IRole>>(assignPopup) {
									private static final long serialVersionUID = 1L;

									@Override
									protected void onShow(AjaxRequestTarget target, IModel<IRole> parameter) {
										assignPopup.init(parameter);
									}
								})
						.addConfirmAction(
								BootstrapRenderer.constant("console.maintenance.infinispan.roles.actions.delete",
										"fa fa-times fa-fw", BootstrapColor.DANGER))
						.title(new ResourceModel("console.maintenance.infinispan.roles.actions.delete.confirm.title"))
						.content(new ResourceModel(
								"console.maintenance.infinispan.roles.actions.delete.confirm.content"))
						.yesNo().onClick(new AbstractOneParameterAjaxAction<IModel<IRole>>() {
							private static final long serialVersionUID = 1L;

							@Override
							public void execute(AjaxRequestTarget target, IModel<IRole> model) {
								try {
									etcdClusterService.unassignRole(model.getObject());
									Session.get().success(
											getString("console.maintenance.infinispan.roles.actions.delete.success"));
									target.add(ConsoleMaintenanceEtcdRolesPanel.this);
								} catch (Exception e) {
									LOGGER.error("Erreur lors de la suppression d'un rôle.");
									Session.get().error(getString("common.error.unexpected"));
								}
								FeedbackUtils.refreshFeedback(target, getPage());
							}
						}).when(new SerializablePredicate<IRole>() {
							private static final long serialVersionUID = 1L;

							public boolean apply(IRole input) {
								RoleEtcdValue roleAttribution = etcdClusterService.getRole(input);
								return roleAttribution != null && roleAttribution.getAttributionDate() != null
										&& roleAttribution.getNodeName() != null;
							}
						}).withClassOnElements("btn-xs").end()
						.bootstrapPanel()
						.title("console.maintenance.etcd.roles")
								.responsive(Condition.alwaysTrue())
								.build("roles")
		);
	}
	
	private class RoleKeyFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		public RoleKeyFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleKey", ConsoleMaintenanceEtcdRolesPanel.this, roleModel);
			
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
		
		private final IModel<RoleEtcdValue> roleAttributionModel;
		
		public RoleAddressFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleAddress", ConsoleMaintenanceEtcdRolesPanel.this, roleModel);
			
			roleAttributionModel = new LoadableDetachableModel<RoleEtcdValue>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected RoleEtcdValue load() {
					return etcdClusterService.getRole(roleModel.getObject());
				}
				
			};
			
			add(
					new CoreLabel("address",
							BindingModel.of(roleAttributionModel, CoreWicketMoreBindings.rodeEtcdValue().nodeName()))
			);
		}
		
		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(roleAttributionModel);
		}
	}

	private class RoleAttributionDateFragment extends Fragment {
		
		private static final long serialVersionUID = 1L;
		
		@SpringBean
		private IEtcdClusterService etcdClusterService;
		
		private final IModel<RoleEtcdValue> roleAttributionModel;
		
		public RoleAttributionDateFragment(String id, IModel<IRole> roleModel) {
			super(id, "roleAttributionDate", ConsoleMaintenanceEtcdRolesPanel.this, roleModel);
			
			roleAttributionModel = new LoadableDetachableModel<RoleEtcdValue>() {
				private static final long serialVersionUID = 1L;
				@Override
				protected RoleEtcdValue load() {
					return etcdClusterService.getRole(roleModel.getObject());
				}
				
			};
			
			add(
					new DateLabel("attributionDate",
							BindingModel.of(roleAttributionModel,
									CoreWicketMoreBindings.rodeEtcdValue().attributionDate()),
							DatePattern.REALLY_SHORT_DATETIME)
			);
		}
		
		@Override
		protected void onDetach() {
			super.onDetach();
			Detachables.detach(roleAttributionModel);
		}
	}
	
	@Override
	protected void onDetach(){
		super.onDetach();
		Detachables.detach(rolesModel);
	}

}
