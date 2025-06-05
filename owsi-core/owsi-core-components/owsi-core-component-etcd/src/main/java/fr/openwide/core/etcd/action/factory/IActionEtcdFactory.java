package fr.openwide.core.etcd.action.factory;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;

public interface IActionEtcdFactory {

	void prepareAction(AbstractEtcdActionValue action);

}
