package fr.openwide.core.etcd.action.model.role.rebalance;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;

/**
 * Rééquilibrage des rôles
 */
public class RoleRebalanceEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = 3717971692955541181L;

	public RoleRebalanceEtcdAction(String target) {
		super(true, target);
	}

	@Override
	protected ActionResultValue doExecute() {
		etcdClusterService.doRebalanceRoles();
		return ActionResultValue.from(Boolean.TRUE);
	}

	public static final RoleRebalanceEtcdAction rebalance(String target) {
		return new RoleRebalanceEtcdAction(target);
	}

}
