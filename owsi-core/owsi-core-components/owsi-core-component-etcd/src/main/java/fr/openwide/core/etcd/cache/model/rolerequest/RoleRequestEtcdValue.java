package fr.openwide.core.etcd.cache.model.rolerequest;

import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

public class RoleRequestEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -8432156130962705416L;

	private String nodeName;

	private RoleRequestEtcdValue(Instant attributionInstant, String nodeName) {
		super(attributionInstant);
		this.nodeName = nodeName;
	}

	public static final RoleRequestEtcdValue from(Date attributionDate, String nodeName) {
		return new RoleRequestEtcdValue(attributionDate.toInstant(), nodeName);
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

}
