package fr.openwide.core.etcd.cache.model;

import java.io.Serializable;
import java.time.Instant;

public interface IEtcdCacheValue extends Serializable {

	Instant getAttributionInstant();

}
