package fr.openwide.core.wicket.more.console.maintenance.etcd.renderer;

import java.util.Locale;
import java.util.Objects;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRenderer;
import fr.openwide.core.wicket.more.markup.html.bootstrap.label.renderer.BootstrapRendererInformation;

public class NodeEtcdRenderer {

//	private static final Renderer<NodeEtcdValue> INSTANCE = new Renderer<NodeEtcdValue>() {
//		private static final long serialVersionUID = 1L;
//		@Override
//		public String render(NodeEtcdValue value, Locale locale) {
//			if (value == null) {
//				return null;
//			}
//			return Joiners.onMiddotSpace()
//					.join(
//							emptyTextToNull(value.get.toString()),
//							emptyTextToNull(value.getName())
//					);
//		}
//	};
//
//	private static final INodeAnonymousRenderer ANONYMOUS = new INodeAnonymousRenderer();
//
//	private static final INodeStatusRenderer STATUS = new INodeStatusRenderer();
//
	private static final NodeEtcdLocalRenderer LOCAL = new NodeEtcdLocalRenderer();

	private static final NodeEtcdMasterRenderer MASTER = new NodeEtcdMasterRenderer();
//
//	public static Renderer<INode> get() {
//		return INSTANCE;
//	}
//
//	public static INodeAnonymousRenderer anonymous() {
//		return ANONYMOUS;
//	}
//
//	public static INodeStatusRenderer status() {
//		return STATUS;
//	}

	public static NodeEtcdLocalRenderer local() {
		return LOCAL;
	}

	public static NodeEtcdMasterRenderer master() {
		return MASTER;
	}

//	private static class INodeAnonymousRenderer extends BootstrapRenderer<INode> {
//		private static final long serialVersionUID = 1L;
//		
//		@Override
//		protected BootstrapRendererInformation doRender(INode value, Locale locale) {
//			if (value == null) {
//				return BootstrapRendererInformation.builder().build();
//			}
//			
//			if (!value.isAnonymous()) {
//				return BootstrapRendererInformation.builder().build();
//			}
//			
//			return BootstrapRendererInformation.builder()
//					.icon("fa fa-exclamation")
//					.color(BootstrapColor.DANGER)
//					.label(getString("business.infinispan.node.anonymous", locale))
//					.build();
//		}
//	}
//
//	private static class INodeStatusRenderer extends BootstrapRenderer<INode> {
//		private static final long serialVersionUID = 1L;
//		
//		@SpringBean
//		private IInfinispanClusterService infinispanClusterService;
//		
//		private boolean initialized;
//		
//		public INodeStatusRenderer() {
//		}
//		
//		@Override
//		protected BootstrapRendererInformation doRender(INode value, Locale locale) {
//			if (!initialized) {
//				Injector.get().inject(this);
//				initialized = true;
//			}
//			
//			if (value == null) {
//				return BootstrapRendererInformation.builder().build();
//			}
//			
//			if (infinispanClusterService.getMembers().contains(value.getAddress())) {
//				return BootstrapRendererInformation.builder()
//						.icon("fa fa-toggle-on")
//						.label(getString("business.infinispan.node.connected", locale))
//						.build();
//			}
//			
//			return BootstrapRendererInformation.builder()
//					.icon("fa fa-toggle-off")
//					.label(getString("business.infinispan.node.disconnected", locale))
//					.build();
//		}
//		
//	}

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

	private static class NodeEtcdMasterRenderer extends BootstrapRenderer<NodeEtcdValue> {
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

			if (Objects.equals(etcdClusterService.getCurrentCoordinator(), value.getNodeName())) {
				return BootstrapRendererInformation.builder().icon("fa fa-star")
						.label(getString("business.etcd.node.master", locale)).build();
			}

			return BootstrapRendererInformation.builder().build();
		}
	}

}

