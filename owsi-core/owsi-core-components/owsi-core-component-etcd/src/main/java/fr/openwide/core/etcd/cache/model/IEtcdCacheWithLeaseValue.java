package fr.openwide.core.etcd.cache.model;

public interface IEtcdCacheWithLeaseValue extends IEtcdCacheValue {

	Long getLeaseId();

	void setLeaseId(Long leaseId);

}
