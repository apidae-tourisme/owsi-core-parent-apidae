package fr.openwide.core.etcd.cache.model.node;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

public class NodeEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -939846120050982502L;

	private Instant leaveDate;

	private String nodeName;

	private NodeEtcdValue(Instant attributionInstant, String nodeName, Instant leaveDate) {
		super(attributionInstant);
		this.leaveDate = leaveDate;
		this.nodeName = nodeName;
	}

	public static final NodeEtcdValue from(Date attributionDate, String nodeName) {
		return new NodeEtcdValue(attributionDate.toInstant(), nodeName, null);
	}

	public Instant getLeaveDate() {
		return leaveDate;
	}

	public void setLeaveDate(Instant leaveDate) {
		this.leaveDate = leaveDate;
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof NodeEtcdValue)) {
			return false;
		}
		NodeEtcdValue other = (NodeEtcdValue) obj;
		if (leaveDate == null) {
			if (other.leaveDate != null) {
				return false;
			}
		} else if (!leaveDate.equals(other.leaveDate)) {
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
		result = prime * result + ((leaveDate == null) ? 0 : leaveDate.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		return result;
	}

}
