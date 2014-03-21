package fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.placeholderpolyfill;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.odlabs.wiquery.core.javascript.JsStatement;

public class PlaceholderPolyfillBehavior extends Behavior {

	private static final long serialVersionUID = 468977788703981632L;

	private static final String PLACEHOLDER = "placeholder";
	
	private static final String DEFAULT_SELECTOR = "[placeholder]";
	
	public PlaceholderPolyfillBehavior() {
		super();
	}
	
	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		response.render(JavaScriptHeaderItem.forReference(PlaceholderPolyfillJavaScriptResourceReference.get()));
		response.render(OnDomReadyHeaderItem.forScript(new JsStatement().$(null, DEFAULT_SELECTOR).chain(PLACEHOLDER).render()));
	}
}
