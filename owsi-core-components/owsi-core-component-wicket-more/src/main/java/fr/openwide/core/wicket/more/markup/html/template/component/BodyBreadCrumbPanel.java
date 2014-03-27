package fr.openwide.core.wicket.more.markup.html.template.component;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import fr.openwide.core.wicket.more.markup.html.basic.ComponentBooleanProperty;
import fr.openwide.core.wicket.more.markup.html.basic.EnclosureBehavior;
import fr.openwide.core.wicket.more.markup.html.template.model.BreadCrumbElement;
import fr.openwide.core.wicket.more.markup.html.template.model.BreadCrumbElementListConcatModel;
import fr.openwide.core.wicket.more.markup.html.template.model.BreadCrumbMarkupTagRenderingBehavior;
import fr.openwide.core.wicket.more.model.ReadOnlyModel;

public class BodyBreadCrumbPanel extends GenericPanel<List<BreadCrumbElement>> {
	
	private static final long serialVersionUID = -3398120588294325073L;
	
	protected IModel<String> dividerModel = ReadOnlyModel.of(Model.of("/"));
	
	private boolean trailingSeparator = false;
	
	public BodyBreadCrumbPanel(
			String id,
			IModel<List<BreadCrumbElement>> prependedBreadCrumbElementsModel,
			IModel<List<BreadCrumbElement>> breadCrumbElementsModel) {
		this(id, prependedBreadCrumbElementsModel, breadCrumbElementsModel, 0);
	}
	
	public BodyBreadCrumbPanel(
			String id,
			IModel<List<BreadCrumbElement>> prependedBreadCrumbElementsModel,
			IModel<List<BreadCrumbElement>> breadCrumbElementsModel, int numberOfElementsToSubstract) {
		super(id, new BreadCrumbElementListConcatModel(prependedBreadCrumbElementsModel, breadCrumbElementsModel, numberOfElementsToSubstract));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new BreadCrumbListView("breadCrumbElementListView", getModel(), BreadCrumbMarkupTagRenderingBehavior.HTML_BODY, dividerModel));
		
		add(new EnclosureBehavior(ComponentBooleanProperty.VISIBLE).collectionModel(getModel()));
		
		add(new WebMarkupContainer("trailingLi") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(trailingSeparator);
			}
		});	
	}
	
	public BodyBreadCrumbPanel setDividerModel(IModel<String> dividerModel) {
		this.dividerModel = dividerModel;
		return this;
	}
	
	public BodyBreadCrumbPanel setTrailingSeparator(boolean trailingSeparator) {
		this.trailingSeparator = trailingSeparator;
		return this;
	}
}