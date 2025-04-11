package fr.openwide.core.etcd.common.service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface IEtcdClusterCheckerService {

	boolean updateCoordinator(String newCoordinator, Collection<String> knownNodes);

	boolean updateCoordinatorTimestamp(String currentCoordinator);

	boolean unsetCoordinator(String oldCoordinator);

	boolean isClusterActive(Collection<String> clusterNodes);

	boolean tryForceUpdate(String currentCoordinator, int delay, TimeUnit unit);

}
