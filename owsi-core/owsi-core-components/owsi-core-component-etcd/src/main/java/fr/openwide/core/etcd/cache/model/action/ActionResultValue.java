package fr.openwide.core.etcd.cache.model.action;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import fr.openwide.core.etcd.cache.model.AbstractEtcdCacheValue;

public class ActionResultValue extends AbstractEtcdCacheValue {

	private static final long serialVersionUID = -1389760128287633827L;

	private Serializable value;

	private ActionResultValue(Instant attributionInstant,Serializable value) {
		super(attributionInstant);
		this.setValue(value);
	}

	public static final ActionResultValue from(Serializable value) {
		return new ActionResultValue(new Date().toInstant(), value);
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ActionResultValue)) {
			return false;
		}
		ActionResultValue other = (ActionResultValue) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

}
