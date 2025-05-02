package fr.openwide.core.etcd.cache.model.role;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheWithLeaseValue;

public class RoleEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue, IEtcdCacheWithLeaseValue {

	private static final long serialVersionUID = 8618813999086906312L;

	private String nodeName;

	private Long leaseId;

	private RoleEtcdValue(Instant attributionInstant, String nodeName) {
		super(attributionInstant);
		this.nodeName = nodeName;
	}

	public static final RoleEtcdValue from(Date attributionDate, String nodeName) {
		return new RoleEtcdValue(attributionDate.toInstant(), nodeName);
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public Long getLeaseId() {
		return leaseId;
	}

	@Override
	public void setLeaseId(Long leaseId) {
		this.leaseId = leaseId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof RoleEtcdValue)) {
			return false;
		}
		RoleEtcdValue other = (RoleEtcdValue) obj;
		if (leaseId == null) {
			if (other.leaseId != null) {
				return false;
			}
		} else if (!leaseId.equals(other.leaseId)) {
			return false;
		}
		if (nodeName == null) {
			if (other.nodeName != null) {
				return false;
			}
		} else if (!nodeName.equals(other.nodeName)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((leaseId == null) ? 0 : leaseId.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		return result;
	}

}
