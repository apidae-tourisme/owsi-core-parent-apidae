package fr.openwide.core.etcd.cache.model.node;

import java.time.Instant;
import java.util.Date;

import org.bindgen.Bindable;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

@Bindable
public class NodeEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {

	private static final long serialVersionUID = -939846120050982502L;

	private String nodeName;

	private NodeEtcdValue(Instant attributionInstant, String nodeName) {
		super(attributionInstant);
		this.nodeName = nodeName;
	}

	public static final NodeEtcdValue from(Date attributionDate, String nodeName) {
		return new NodeEtcdValue(attributionDate.toInstant(), nodeName);
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
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return String.format("<%s (at %tF %<tT %<tz)>", getNodeName(), getAttributionDate());
	}

}
