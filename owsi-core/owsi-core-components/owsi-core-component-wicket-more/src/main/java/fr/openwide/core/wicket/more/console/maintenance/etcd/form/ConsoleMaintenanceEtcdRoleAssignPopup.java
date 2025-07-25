package fr.openwide.core.wicket.more.console.maintenance.etcd.form;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.etcd.action.model.role.SwitchRoleResultActionResult;
import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.infinispan.action.SwitchRoleResult;
import fr.openwide.core.infinispan.model.IRole;
import fr.openwide.core.wicket.more.console.maintenance.etcd.component.roles.ConsoleMaintenanceEtcdRolesPanel;
import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.markup.html.link.BlankLink;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.component.AbstractAjaxModalPopupPanel;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.component.DelegatedMarkupPanel;
import fr.openwide.core.wicket.more.util.model.Detachables;

public class ConsoleMaintenanceEtcdRoleAssignPopup extends AbstractAjaxModalPopupPanel<IRole> {

	private static final long serialVersionUID = -2999208922165619653L;
	
	public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMaintenanceEtcdRoleAssignPopup.class);
	
	@SpringBean
	private IEtcdClusterService etcdClusterService;
	
	private final Form<IRole> form;

	private IModel<NodeEtcdValue> nodeModel = Model.of();

	public ConsoleMaintenanceEtcdRoleAssignPopup(String id) {
		super(id, new Model<IRole>());
		
		this.form = new Form<>("form", getModel());
	}

	@Override
	protected Component createHeader(String wicketId) {
		DelegatedMarkupPanel header = new DelegatedMarkupPanel(wicketId, getClass());
		return header;
	}

	@Override
	protected Component createBody(String wicketId) {
		DelegatedMarkupPanel body = new DelegatedMarkupPanel(wicketId, getClass());
		
		body.add(form);
		
		form.add(
				new EtcdNodeDropDownSingleChoice("node", nodeModel)
						.setRequired(true)
						.setLabel(new ResourceModel("business.etcd.role.node"))
		);
		
		return body;
	}

	@Override
	protected Component createFooter(String wicketId) {
		DelegatedMarkupPanel footer = new DelegatedMarkupPanel(wicketId, getClass());
		
		footer.add(
				new AjaxButton("save", form) {
					private static final long serialVersionUID = 1L;
					
					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						try {
							SwitchRoleResultActionResult result = etcdClusterService.assignRole(
									ConsoleMaintenanceEtcdRoleAssignPopup.this.getModelObject(), nodeModel.getObject());
							if (SwitchRoleResult.SWITCH_SUCCESS == result.getSwitchRoleResult()) {
								Session.get()
										.success(getString("console.maintenance.etcd.roles.actions.assign.success"));
							} else {
								Session.get().error(String.format("Erreur : %s", result.getMessage()));
							}
							closePopup(target);
							target.addChildren(getPage(), ConsoleMaintenanceEtcdRolesPanel.class);
						} catch (Exception e) {
							LOGGER.error("Erreur lors de l'affectation d'un rôle.", e);
							Session.get().error(getString("common.error.unexpected"));
						}
						FeedbackUtils.refreshFeedback(target, getPage());
					}
				}
		);
		
		BlankLink cancel = new BlankLink("cancel");
		addCancelBehavior(cancel);
		footer.add(cancel);
		
		return footer;
	}

	public void init(IModel<IRole> model) {
		getModel().setObject(model.getObject());
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		Detachables.detach(nodeModel);
	}

}
