package fr.openwide.core.infinispan.service;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;

import fr.openwide.core.infinispan.utils.GlobalDefaultReplicatedTransientConfigurationBuilder;

public class CustomConfigurationBuilderHolder extends ConfigurationBuilderHolder {
	
	/**
	 * Maximum time to attempt a particular lock acquisition
	 */
	private final int lockAcquisitionTimeout;
	
    /**
     * This is the timeout used to wait for an acknowledgment when making a remote call, after which
     * the call is aborted and an exception is thrown.
     */
	private final int remoteTimeout;

	public CustomConfigurationBuilderHolder(Properties transportProperties, int lockAcquisitionTimeout, int remoteTimeout) {
		super(Thread.currentThread().getContextClassLoader(), new GlobalDefaultReplicatedTransientConfigurationBuilder(transportProperties));
		this.lockAcquisitionTimeout = lockAcquisitionTimeout;
		this.remoteTimeout = remoteTimeout;
	}
	
	@Override
	public GlobalDefaultReplicatedTransientConfigurationBuilder getGlobalConfigurationBuilder() {
		return (GlobalDefaultReplicatedTransientConfigurationBuilder) super.getGlobalConfigurationBuilder();
	}

	@Override
	public ConfigurationBuilder newConfigurationBuilder(String name) {
		ConfigurationBuilder builder = super.newConfigurationBuilder(name);
		builder.clustering()
			// synchronous with l1 cache
			.cacheMode(CacheMode.REPL_SYNC)
			.remoteTimeout(remoteTimeout, TimeUnit.SECONDS)
			.expiration().lifespan(-1)
			// transactional (to allow block locking)
			.transaction().lockingMode(LockingMode.PESSIMISTIC)
			.locking().isolationLevel(IsolationLevel.REPEATABLE_READ).lockAcquisitionTimeout(lockAcquisitionTimeout, TimeUnit.SECONDS)
			// enable batch (to allow block locking)
			.invocationBatching().enable()
			.jmxStatistics();
		return builder;
	}
}