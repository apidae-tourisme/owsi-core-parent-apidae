package fr.openwide.core.etcd.coordinator.service;

import fr.openwide.core.etcd.common.exception.EtcdServiceException;

public interface IEtcdCoordinatorService {

	void start();

	void stop();

	boolean tryBecomeCoordinator();

	String getCurrentCoordinator() throws EtcdServiceException;

	boolean isCoordinator();

	boolean isClusterActive();

	boolean deleteCoordinator() throws EtcdServiceException;

	boolean deleteCoordinatorKeyIfOwned();

}
