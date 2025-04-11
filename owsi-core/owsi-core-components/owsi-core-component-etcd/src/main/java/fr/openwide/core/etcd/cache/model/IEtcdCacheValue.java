package fr.openwide.core.etcd.cache.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

public interface IEtcdCacheValue extends Serializable {

	Date getAttributionDate();

	Instant getAttributionInstant();


}
