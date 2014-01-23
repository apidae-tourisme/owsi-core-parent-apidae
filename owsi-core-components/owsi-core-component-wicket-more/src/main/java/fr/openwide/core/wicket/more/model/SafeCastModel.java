package fr.openwide.core.wicket.more.model;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import fr.openwide.core.jpa.util.HibernateUtils;

public class SafeCastModel<T> extends AbstractReadOnlyModel<T> {
	
	private static final long serialVersionUID = 4014147784926275050L;

	private final Class<T> targetClass;
	
	private final IModel<?> wrappedModel;
	
	public static <T> SafeCastModel<T> of(Class<T> targetClass, IModel<?> model) {
		return new SafeCastModel<T>(targetClass, model);
	}

	protected SafeCastModel(Class<T> targetClass, IModel<?> wrappedModel) {
		super();
		this.targetClass = targetClass;
		this.wrappedModel = wrappedModel;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getObject() {
		Object object = wrappedModel.getObject();
		if (object == null) {
			return null;
		}
		if (!targetClass.isAssignableFrom(HibernateUtils.getClass(object))) {
			return null;
		}
		return HibernateUtils.unwrap((T) object);
	}
	
	@Override
	public void detach() {
		wrappedModel.detach();
	}
}
