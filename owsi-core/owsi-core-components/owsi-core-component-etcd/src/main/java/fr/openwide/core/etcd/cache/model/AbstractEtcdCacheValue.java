package fr.openwide.core.etcd.cache.model;

import java.time.Instant;
import java.util.Date;

public abstract class AbstractEtcdCacheValue implements IEtcdCacheValue {

	private static final long serialVersionUID = -372950186993356350L;

	private Instant attributionInstant;

	protected AbstractEtcdCacheValue(Instant attributionInstant) {
		super();
		this.attributionInstant = attributionInstant;
	}

	@Override
	public Instant getAttributionInstant() {
		return attributionInstant;
	}

	public void setAttributionInstant(Instant attributionInstant) {
		this.attributionInstant = attributionInstant;
	}

	@Override
	public Date getAttributionDate() {
		return attributionInstant != null ? Date.from(attributionInstant) : null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AbstractEtcdCacheValue other = (AbstractEtcdCacheValue) obj;
		if (attributionInstant == null) {
			if (other.attributionInstant != null) {
				return false;
			}
		} else if (!attributionInstant.equals(other.attributionInstant)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributionInstant == null) ? 0 : attributionInstant.hashCode());
		return result;
	}

}
