package fr.openwide.core.etcd.lock.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bindgen.Bindable;

import fr.openwide.core.etcd.cache.model.lockattribution.LockAttributionEtcdValue;

@Bindable
public class EtcdLock implements Serializable {

	private static final long serialVersionUID = -2703012259547691866L;

	private final String fullKey;

	private final String key;

	private LockAttributionEtcdValue lockAttributionEtcdValue;

	private EtcdLock(String fullKey, String key) {
		super();
		this.key = key;
		this.fullKey = fullKey;
	}

	public String getKey() {
		return key;
	}

	public String getFullKey() {
		return fullKey;
	}

	@Override
	public String toString() {
		return String.format("Lock<%s>", fullKey);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(fullKey)
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		EtcdLock etcdLock = (EtcdLock) obj;
		return java.util.Objects.equals(fullKey, etcdLock.fullKey);
	}

	public static final EtcdLock from(String fullKey, String key) {
		return new EtcdLock(fullKey, key);
	}

	public LockAttributionEtcdValue getLockAttributionEtcdValue() {
		return lockAttributionEtcdValue;
	}

	public void setLockAttributionEtcdValue(LockAttributionEtcdValue lockAttributionEtcdValue) {
		this.lockAttributionEtcdValue = lockAttributionEtcdValue;
	}

}
