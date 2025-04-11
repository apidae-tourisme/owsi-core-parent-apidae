package fr.openwide.core.etcd.action.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;

public interface IEtcdActionService {

	void start();
    
	void stop();

	String resultLessAction(AbstractEtcdActionValue action) throws EtcdServiceException;
    
	<T> T syncedAction(AbstractEtcdActionValue action, int timeout, TimeUnit unit)
			throws EtcdServiceException, ExecutionException, TimeoutException;
    
	void processAction(String actionId) throws EtcdServiceException;
} 