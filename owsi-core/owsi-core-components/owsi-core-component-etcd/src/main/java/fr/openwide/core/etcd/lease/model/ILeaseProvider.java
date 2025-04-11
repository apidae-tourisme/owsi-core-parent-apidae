package fr.openwide.core.etcd.lease.model;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import io.etcd.jetcd.lease.LeaseRevokeResponse;

public interface ILeaseProvider {

	Long getGlobalClientLeaseIdWithKeepAlive() throws EtcdServiceException;

	Long getNewLeaseIdWithKeepAlive() throws EtcdServiceException;

	LeaseRevokeResponse revokeLease(long leaseId) throws EtcdServiceException;

}
