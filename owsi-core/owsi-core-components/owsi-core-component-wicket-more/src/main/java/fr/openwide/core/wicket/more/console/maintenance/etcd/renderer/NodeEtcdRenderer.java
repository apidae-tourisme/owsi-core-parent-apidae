package fr.openwide.core.wicket.more.console.maintenance.etcd.renderer;

import java.util.Locale;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRenderer;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRendererInformation;

public class NodeEtcdRenderer {

	private static final NodeEtcdLocalRenderer LOCAL = new NodeEtcdLocalRenderer();

	public static NodeEtcdLocalRenderer local() {
		return LOCAL;
	}

	private static class NodeEtcdLocalRenderer extends BootstrapRenderer<NodeEtcdValue> {
		private static final long serialVersionUID = 1L;
		
		@SpringBean
		private IEtcdClusterService etcdClusterService;
		
		private boolean initialized;
		
		@Override
		protected BootstrapRendererInformation doRender(NodeEtcdValue value, Locale locale) {
			if (!initialized) {
				Injector.get().inject(this);
				initialized = true;
			}
			
			if (etcdClusterService.getNodeName().equals(value.getNodeName())) {
				return BootstrapRendererInformation.builder()
						.icon("fa fa-user")
						.label(getString("business.etcd.node.local", locale))
						.build();
			}
			
			return BootstrapRendererInformation.builder().build();
		}
	}

}

