package fr.openwide.core.etcd.master.service;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;

public interface IEtcdMasterService {

	void start();

	boolean tryBecomeMaster();

	String getCurrentMaster() throws EtcdServiceException;

	boolean isMaster();

}
