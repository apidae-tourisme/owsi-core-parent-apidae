package fr.openwide.core.etcd.cache.model.priorityqueue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

public class PriorityQueueEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {
    private static final long serialVersionUID = 1L;

    private final String nodeName;
	private final List<RoleAttribution> attributions;

	private PriorityQueueEtcdValue(Instant attributionInstant, String nodeName, List<RoleAttribution> attributions) {
        super(attributionInstant);
        this.nodeName = nodeName;
        this.attributions = attributions;
    }

	public static PriorityQueueEtcdValue from(Date date, String nodeName, List<RoleAttribution> attributions) {
        return new PriorityQueueEtcdValue(date.toInstant(), nodeName, attributions);
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

	public List<RoleAttribution> getAttributions() {
        return attributions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof PriorityQueueEtcdValue)) {
            return false;
        }
        PriorityQueueEtcdValue other = (PriorityQueueEtcdValue) obj;
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
        result = prime * result + ((attributions == null) ? 0 : attributions.hashCode());
        return result;
    }
} 