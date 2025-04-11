package fr.openwide.core.etcd.cache.model.role;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

public class RoleEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = 8618813999086906312L;

	private String nodeName;

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

}
