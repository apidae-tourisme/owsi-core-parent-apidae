package fr.openwide.core.etcd.action.model.role.release;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.infinispan.model.IRole;

/**
 * Libération de rôles
 */
public class RoleReleaseEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = -276444744888760127L;

	private final IRole role;

	public RoleReleaseEtcdAction(String target, IRole role) {
		super(false, target);
		this.role = role;
	}

	public IRole getRole() {
		return role;
	}

	@Override
	protected ActionResultValue doExecute() {
		return ActionResultValue.from(etcdClusterService.doReleaseRole(role));
	}

	public static final RoleReleaseEtcdAction release(String target, IRole role) {
		return new RoleReleaseEtcdAction(target, role);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof RoleReleaseEtcdAction)) {
			return false;
		}
		RoleReleaseEtcdAction other = (RoleReleaseEtcdAction) obj;
		if (role == null) {
			if (other.role != null) {
				return false;
			}
		} else if (!role.equals(other.role)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		return result;
	}

}
