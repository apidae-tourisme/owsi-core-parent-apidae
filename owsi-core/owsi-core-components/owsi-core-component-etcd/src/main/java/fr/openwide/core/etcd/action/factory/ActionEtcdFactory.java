package fr.openwide.core.etcd.action.factory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;

public class ActionEtcdFactory implements IActionEtcdFactory {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public void prepareAction(AbstractEtcdActionValue action) {
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(action, Autowire.BY_TYPE.value(),
				true);
	}

}
