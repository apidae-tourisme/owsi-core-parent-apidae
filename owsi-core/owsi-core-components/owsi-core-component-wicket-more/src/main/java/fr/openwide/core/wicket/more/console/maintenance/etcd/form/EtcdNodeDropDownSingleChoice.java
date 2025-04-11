package fr.openwide.core.wicket.more.console.maintenance.etcd.form;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import fr.openwide.core.etcd.cache.model.node.NodeEtcdValue;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.wicket.more.markup.html.select2.GenericSelect2DropDownSingleChoice;

public class EtcdNodeDropDownSingleChoice extends GenericSelect2DropDownSingleChoice<NodeEtcdValue> {

	private static final long serialVersionUID = -6340793244408907094L;

	@SpringBean
	private IEtcdClusterService etcdClusterService;

	private static final EtcdNodeChoiceRenderer CHOICE_RENDERER = new EtcdNodeChoiceRenderer();

	protected EtcdNodeDropDownSingleChoice(String id, IModel<NodeEtcdValue> model) {
		super(
				id,
				model,
				new ListModel<>(),
				CHOICE_RENDERER
		);
		setChoices(
				new LoadableDetachableModel<List<NodeEtcdValue>>() {
					private static final long serialVersionUID = 1L;
					@Override
					protected List<NodeEtcdValue> load() {
						return new ArrayList<>(etcdClusterService.getNodes().values());
					}
				}
		);
	}

	private static class EtcdNodeChoiceRenderer extends ChoiceRenderer<NodeEtcdValue> {
		private static final long serialVersionUID = 1L;
		@Override
		public Object getDisplayValue(NodeEtcdValue object) {
			return object.getNodeName();
		}
		@Override
		public String getIdValue(NodeEtcdValue object, int index) {
			return object.getNodeName();
		}
	}

}
