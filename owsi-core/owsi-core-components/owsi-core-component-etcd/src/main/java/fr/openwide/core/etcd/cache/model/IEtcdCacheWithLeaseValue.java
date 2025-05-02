package fr.openwide.core.etcd.cache.model;

public interface IEtcdCacheWithLeaseValue extends IEtcdCacheNodeValue {

	Long getLeaseId();

	void setLeaseId(Long leaseId);

}
