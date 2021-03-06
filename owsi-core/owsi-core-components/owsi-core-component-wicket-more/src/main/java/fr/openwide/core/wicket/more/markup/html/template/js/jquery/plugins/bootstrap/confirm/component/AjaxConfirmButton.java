package fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.confirm.statement.BootstrapConfirmStatement;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.BootstrapModalJavaScriptResourceReference;

public abstract class AjaxConfirmButton extends AjaxButton {

	private static final long serialVersionUID = -132330109149500197L;

	@Deprecated
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel) {
		this(
				id,
				titleModel,
				textModel,
				yesLabelModel,
				noLabelModel,
				new Model<String>("icon-ok icon-white fa fa-check"),
				new Model<String>("icon-ban-circle fa fa-ban"),
				new Model<String>("btn btn-success"),
				new Model<String>("btn btn-default"),
				null,
				false,
				null
		);
	}
	
	@Deprecated
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> yesIconModel, IModel<String> noIconModel,
			IModel<String> yesButtonModel, IModel<String> noButtonModel) {
		this(id, titleModel, textModel, yesLabelModel, noLabelModel, yesIconModel, noIconModel, yesButtonModel, noButtonModel, null, false, null);
	}

	@Deprecated
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, Form<?> form) {
		this(
				id,
				titleModel,
				textModel,
				yesLabelModel,
				noLabelModel,
				new Model<String>("icon-ok icon-white fa fa-check"),
				new Model<String>("icon-ban-circle fa fa-ban"),
				new Model<String>("btn btn-success"),
				new Model<String>("btn btn-default"),
				null,
				false,
				form
		);
	}
	
	@Deprecated
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> yesIconModel, IModel<String> noIconModel,
			IModel<String> yesButtonModel, IModel<String> noButtonModel, Form<?> form) {
		this(id, titleModel, textModel, yesLabelModel, noLabelModel, yesIconModel, noIconModel, yesButtonModel, noButtonModel, null, false, form);
	}

	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel,
			IModel<String> cssClassNamesModel, boolean textNoEscape) {
		this(
				id,
				titleModel,
				textModel,
				yesLabelModel,
				noLabelModel,
				new Model<String>("icon-ok icon-white fa fa-check"),
				new Model<String>("icon-ban-circle fa fa-ban"),
				new Model<String>("btn btn-success"),
				new Model<String>("btn btn-default"),
				cssClassNamesModel,
				textNoEscape,
				null
		);
	}
	
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> yesIconModel, IModel<String> noIconModel,
			IModel<String> yesButtonModel, IModel<String> noButtonModel,
			IModel<String> cssClassNamesModel, boolean textNoEscape) {
		this(id, titleModel, textModel, yesLabelModel, noLabelModel, yesIconModel, noIconModel,
				yesButtonModel, noButtonModel, cssClassNamesModel, textNoEscape, null);
	}

	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel,
			IModel<String> cssClassNamesModel, boolean textNoEscape, Form<?> form) {
		this(
				id,
				titleModel,
				textModel,
				yesLabelModel,
				noLabelModel,
				new Model<String>("icon-ok icon-white fa fa-check"),
				new Model<String>("icon-ban-circle fa fa-ban"),
				new Model<String>("btn btn-success"),
				new Model<String>("btn btn-default"),
				cssClassNamesModel,
				textNoEscape,
				form
		);
	}
	public AjaxConfirmButton(String id, IModel<String> titleModel, IModel<String> textModel,
			IModel<String> yesLabelModel, IModel<String> noLabelModel, IModel<String> yesIconModel, IModel<String> noIconModel,
			IModel<String> yesButtonModel, IModel<String> noButtonModel,
			IModel<String> cssClassNamesModel, boolean textNoEscape, Form<?> form) {
		super(id, null, form);
		add(new ConfirmContentBehavior(titleModel, textModel, yesLabelModel, noLabelModel, yesIconModel, noIconModel,
				yesButtonModel, noButtonModel, cssClassNamesModel, textNoEscape));

		// Lors du clic, on ouvre la popup de confirmation. Si l'action est confirm??e, 
		// on d??lenche un ??v??nement 'confirm'.
		// l'??v??nement click habituel est supprim?? par surcharge de newAjaxEventBehavior ci-dessous
		Event clickEvent = new Event(MouseEvent.CLICK) {
			private static final long serialVersionUID = 1L;
			@Override
			public JsScope callback() {
				return JsScopeEvent.quickScope(BootstrapConfirmStatement.confirm(AjaxConfirmButton.this).append("event.preventDefault();"));
			}
		};
		add(new WiQueryEventBehavior(clickEvent) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isEnabled(Component component) {
				return AjaxConfirmButton.this.isEnabledInHierarchy();
			}
		});
	}

	/**
	 * Cette m??thode fournit normalement le handler pour l'??v??nement onclick. On le remplace par l'??v??nement de
	 * confirmation (le onclick est g??r?? sans ajax au-dessus).
	 */
	@Override
	protected AjaxFormSubmitBehavior newAjaxFormSubmitBehavior(String event) {
		return new AjaxFormSubmitBehavior(getForm(), "confirm") {
			private static final long serialVersionUID = 1L;
			
			@Override
			public boolean isEnabled(Component component) {
				// On ajoute le handler seulement si le composant est activ??
				return AjaxConfirmButton.this.isEnabledInHierarchy();
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				AjaxConfirmButton.this.onSubmit(target, AjaxConfirmButton.this.getForm());
			}

			@Override
			protected void onAfterSubmit(AjaxRequestTarget target) {
				AjaxConfirmButton.this.onAfterSubmit(target, AjaxConfirmButton.this.getForm());
			}

			@Override
			protected void onError(AjaxRequestTarget target) {
				AjaxConfirmButton.this.onError(target, AjaxConfirmButton.this.getForm());
			}

			// TODO 0.10 : checker avec LAL ou YRO
//			@SuppressWarnings("deprecation")
//			@Override
//			protected AjaxChannel getChannel() {
//				return AjaxConfirmButton.this.getChannel();
//			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				AjaxConfirmButton.this.updateAjaxAttributes(attributes);
			}

			@Override
			public boolean getDefaultProcessing() {
				return AjaxConfirmButton.this.getDefaultFormProcessing();
			}
		};
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(BootstrapModalJavaScriptResourceReference.get()));
	}

}
