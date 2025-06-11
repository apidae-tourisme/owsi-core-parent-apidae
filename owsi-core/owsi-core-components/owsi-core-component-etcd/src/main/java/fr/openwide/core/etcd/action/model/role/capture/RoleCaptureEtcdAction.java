package fr.openwide.core.etcd.action.model.role.capture;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.infinispan.model.IRole;

/**
 * Capture de rôles
 */
public class RoleCaptureEtcdAction extends AbstractEtcdActionValue {

	private static final long serialVersionUID = 7092801981061903989L;

	private final IRole role;

	public RoleCaptureEtcdAction(String target, IRole role) {
		super(false, target);
		this.role = role;
	}

	public IRole getRole() {
		return role;
	}

	@Override
	protected ActionResultValue doExecute() {
		return ActionResultValue.from(etcdClusterService.doCaptureRole(role));
	}

	public static final RoleCaptureEtcdAction capture(String target, IRole role) {
		return new RoleCaptureEtcdAction(target, role);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof RoleCaptureEtcdAction)) {
			return false;
		}
		RoleCaptureEtcdAction other = (RoleCaptureEtcdAction) obj;
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
