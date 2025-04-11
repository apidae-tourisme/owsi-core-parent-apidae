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

}
