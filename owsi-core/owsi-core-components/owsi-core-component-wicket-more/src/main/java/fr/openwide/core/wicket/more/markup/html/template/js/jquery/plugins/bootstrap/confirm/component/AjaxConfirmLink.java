package fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.wiquery.core.events.Event;
import org.wicketstuff.wiquery.core.events.MouseEvent;
import org.wicketstuff.wiquery.core.events.WiQueryEventBehavior;
import org.wicketstuff.wiquery.core.javascript.JsScope;
import org.wicketstuff.wiquery.core.javascript.JsScopeEvent;

import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.behavior.ConfirmContentBehavior;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.fluid.IConfirmLinkBuilderStepStart;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.statement.BootstrapConfirmStatement;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.BootstrapModalJavaScriptResourceReference;

public abstract class AjaxConfirmLink<O> extends AjaxLink<O> {

	private static final long serialVersionUID = -645345859108195615L;

	public static <O> IConfirmLinkBuilderStepStart<AjaxConfirmLink<O>, O> build() {
		return new AjaxConfirmLinkBuilder<O>();
	}

	private final Form<?> form;

	protected AjaxConfirmLink(String id, IModel<O> model, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> cssClassNamesModel, boolean textNoEscape) {
		this(
				id,
				model,
				null,
				titleModel,
				textModel,
				yesLabelModel,
				noLabelModel,
				new Model<String>("icon-ok icon-white fa fa-check"),
				new Model<String>("icon-ban-circle fa fa-ban"),
				new Model<String>("btn btn-success"),
				new Model<String>("btn btn-default"),
				cssClassNamesModel,
				textNoEscape
		);
	}
	
	protected AjaxConfirmLink(String id, IModel<O> model, Form<?> form, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> yesIconModel, IModel<String> noIconModel,
			IModel<String> yesButtonModel, IModel<String> noButtonModel, IModel<String> cssClassNamesModel, boolean textNoEscape) {
		super(id, model);
		setOutputMarkupId(true);
		this.form = form;
		add(new ConfirmContentBehavior(titleModel, textModel, yesLabelModel, noLabelModel, yesIconModel, noIconModel,
				yesButtonModel, noButtonModel, cssClassNamesModel, textNoEscape));
		
		// Lors du clic, on ouvre la popup de confirmation. Si l'action est confirm??e, 
		// on d??lenche un ??v??nement 'confirm'.
		// l'??v??nement click habituel est supprim?? par surcharge de newAjaxEventBehavior ci-dessous
		Event clickEvent = new Event(MouseEvent.CLICK) {
			private static final long serialVersionUID = 1L;
			@Override
			public JsScope callback() {
				return JsScopeEvent.quickScope(BootstrapConfirmStatement.confirm(AjaxConfirmLink.this).append("event.preventDefault();"));
			}
		};
		add(new WiQueryEventBehavior(clickEvent) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isEnabled(Component component) {
				return AjaxConfirmLink.this.isEnabledInHierarchy();
			}
		});
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(BootstrapModalJavaScriptResourceReference.get()));
	}

	/**
	 * Cette m??thode fournit normalement le handler pour l'??v??nement onclick. On le remplace par l'??v??nement de
	 * confirmation (le onclick est g??r?? sans ajax au-dessus).
	 */
	@Override
	protected AjaxEventBehavior newAjaxEventBehavior(String event) {
		if (form != null) {
			return new AjaxFormSubmitBehavior(form, "confirm") {
				private static final long serialVersionUID = 4405251450215656630L;
				
				@Override
				protected void onSubmit(AjaxRequestTarget target) {
					onClick(target);
				}
				
				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
					super.updateAjaxAttributes(attributes);
					AjaxConfirmLink.this.updateAjaxAttributes(attributes);
				}
			};
		} else {
			// Lorsque l'??v??nement 'confirm' est d??tect??, on d??clenche l'action ?? proprement parler.
			return new AjaxEventBehavior("confirm") {
				private static final long serialVersionUID = 1L;
				
				@Override
				public boolean isEnabled(Component component) {
					// On ajoute le handler seulement si le lien est activ??
					return AjaxConfirmLink.this.isEnabledInHierarchy();
				}
				
				@Override
				protected void onEvent(AjaxRequestTarget target) {
					onClick(target);
				}
				
				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
					super.updateAjaxAttributes(attributes);
					AjaxConfirmLink.this.updateAjaxAttributes(attributes);
				}
			};
		}
	}
}
