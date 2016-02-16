package fr.openwide.core.basicapp.web.application.common.template;

import static fr.openwide.core.basicapp.web.application.property.BasicApplicationWebappPropertyIds.MAINTENANCE_URL;
import static fr.openwide.core.jpa.more.property.JpaMorePropertyIds.MAINTENANCE;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.basicapp.web.application.BasicApplicationSession;
import fr.openwide.core.basicapp.web.application.common.template.styles.ServiceLessCssResourceReference;
import fr.openwide.core.jpa.security.service.IAuthenticationService;
import fr.openwide.core.spring.property.service.IPropertyService;
import fr.openwide.core.wicket.markup.html.basic.CoreLabel;
import fr.openwide.core.wicket.markup.html.panel.InvisiblePanel;
import fr.openwide.core.wicket.more.markup.html.feedback.AnimatedGlobalFeedbackPanel;
import fr.openwide.core.wicket.more.markup.html.template.AbstractWebPageTemplate;
import fr.openwide.core.wicket.more.markup.html.template.model.BreadCrumbElement;

public abstract class ServiceTemplate extends AbstractWebPageTemplate {

	private static final long serialVersionUID = 3342562716259012460L;

	@SpringBean
	private IPropertyService propertyService;

	@SpringBean
	private IAuthenticationService authenticationService;

	public ServiceTemplate(PageParameters parameters) {
		super(parameters);
		
		if (Boolean.TRUE.equals(propertyService.get(MAINTENANCE)) && !authenticationService.hasAdminRole() && maintenanceRestriction()) {
			throw new RedirectToUrlException(propertyService.get(MAINTENANCE_URL));
		}
		
		add(new AnimatedGlobalFeedbackPanel("feedback"));
		
		add(new TransparentWebMarkupContainer("htmlRootElement")
				.add(AttributeAppender.append("lang", BasicApplicationSession.get().getLocale().getLanguage())));
		
		addHeadPageTitlePrependedElement(new BreadCrumbElement(new ResourceModel("common.rootPageTitle")));
		add(createHeadPageTitle("headPageTitle"));
		
		add(new CoreLabel("title", getTitleModel()));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(getIntroComponent("intro"));
		add(getContentComponent("content"));
		add(getFooterComponent("footer"));
	}

	protected abstract IModel<String> getTitleModel();

	protected Component getIntroComponent(String wicketId) {
		return new InvisiblePanel(wicketId);
	}

	protected abstract Component getContentComponent(String wicketId);

	protected Component getFooterComponent(String wicketId) {
		return new InvisiblePanel(wicketId);
	}

	protected boolean maintenanceRestriction() {
		return true;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(Application.get().getJavaScriptLibrarySettings().getJQueryReference()));
		response.render(CssHeaderItem.forReference(ServiceLessCssResourceReference.get()));
	}

	@Override
	protected Class<? extends WebPage> getFirstMenuPage() {
		return null;
	}

	@Override
	protected Class<? extends WebPage> getSecondMenuPage() {
		return null;
	}

}