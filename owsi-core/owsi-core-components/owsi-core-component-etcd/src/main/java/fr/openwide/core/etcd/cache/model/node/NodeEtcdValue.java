package fr.openwide.core.etcd.cache.model.node;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheWithLeaseValue;

public class NodeEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue, IEtcdCacheWithLeaseValue {

	private static final long serialVersionUID = -939846120050982502L;

	private Instant leaveDate;

	private Long leaseId;

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
	public Long getLeaseId() {
		return leaseId;
	}

	@Override
	public void setLeaseId(Long leaseId) {
		this.leaseId = leaseId;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

}
