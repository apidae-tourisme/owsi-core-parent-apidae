package fr.openwide.core.etcd.cache.model.lockattribution;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.bindgen.Bindable;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IPriorityQueue;
import fr.openwide.core.infinispan.model.IRole;

@Bindable
public class LockAttributionEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -939846120050982502L;

	private String nodeName;

	private String role;

	private String priorityQueue;

	private LockAttributionEtcdValue(Instant attributionInstant, String nodeName, String role, String priorityQueue) {
		super(attributionInstant);
		this.nodeName = nodeName;
		this.role = role;
		this.priorityQueue = priorityQueue;
	}

	public static LockAttributionEtcdValue from(String nodeName, ILockRequest lockRequest) {
		String role = Optional.ofNullable(lockRequest.getRole()).map(IRole::getKey).orElse(null);
		String priorityQueue = Optional.ofNullable(lockRequest.getPriorityQueue()).map(IPriorityQueue::getKey)
				.orElse(null);
		return from(new Date(), nodeName, role, priorityQueue);
	}

	public static final LockAttributionEtcdValue from(String nodeName, String role, String priorityQueue) {
		return from(new Date(), nodeName, role, priorityQueue);
	}

	private static final LockAttributionEtcdValue from(Date attributionDate, String nodeName, String role,
			String priorityQueue) {
		return new LockAttributionEtcdValue(attributionDate.toInstant(), nodeName, role, priorityQueue);
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getPriorityQueue() {
		return priorityQueue;
	}

	public void setPriorityQueue(String priorityQueue) {
		this.priorityQueue = priorityQueue;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof LockAttributionEtcdValue)) {
			return false;
		}
		LockAttributionEtcdValue other = (LockAttributionEtcdValue) obj;
		if (nodeName == null) {
			if (other.nodeName != null) {
				return false;
			}
		} else if (!nodeName.equals(other.nodeName)) {
			return false;
		}
		if (role == null) {
			if (other.role != null) {
				return false;
			}
		} else if (!role.equals(other.role)) {
			return false;
		}
		if (priorityQueue == null) {
			if (other.priorityQueue != null) {
				return false;
			}
		} else if (!priorityQueue.equals(other.priorityQueue)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((priorityQueue == null) ? 0 : priorityQueue.hashCode());
		return result;
	}

}
