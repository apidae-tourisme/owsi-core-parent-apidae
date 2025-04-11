package fr.openwide.core.etcd.cache.model.priorityqueue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bindgen.Bindable;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;
import fr.openwide.core.etcd.cache.model.IEtcdCacheNodeValue;

@Bindable
public class PriorityQueueEtcdValue extends AbstractEtcdCacheValue implements IEtcdCacheNodeValue {
    private static final long serialVersionUID = 1L;

    private final String nodeName;
    private final String priorityQueueKey;
	private final List<RoleAttribution> attributions;

	private PriorityQueueEtcdValue(Instant attributionInstant, String nodeName, String priorityQueueKey, List<RoleAttribution> attributions) {
        super(attributionInstant);
        this.nodeName = nodeName;
        this.priorityQueueKey = priorityQueueKey;
        this.attributions = attributions;
    }

	public static PriorityQueueEtcdValue from(Date date, String nodeName, String priorityQueueKey, List<RoleAttribution> attributions) {
        return new PriorityQueueEtcdValue(date.toInstant(), nodeName, priorityQueueKey, attributions);
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

	public String getPriorityQueueKey() {
        return priorityQueueKey;
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
        if (priorityQueueKey == null) {
            if (other.priorityQueueKey != null) {
                return false;
            }
        } else if (!priorityQueueKey.equals(other.priorityQueueKey)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
        result = prime * result + ((priorityQueueKey == null) ? 0 : priorityQueueKey.hashCode());
        result = prime * result + ((attributions == null) ? 0 : attributions.hashCode());
        return result;
    }
} 