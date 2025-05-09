package fr.openwide.core.jpa.more.business.task.service.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.infinispan.model.ILock;
import fr.openwide.core.infinispan.model.ILockRequest;
import fr.openwide.core.infinispan.model.IPriorityQueue;
import fr.openwide.core.infinispan.model.impl.LockRequest;
import fr.openwide.core.infinispan.service.IInfinispanClusterService;

/**
 * A consumer thread with the ability to try stopping gracefully.
 * @see #stop(int)
 */
class ConsumerInfinispanAwareThread extends ConsumerThread {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerInfinispanAwareThread.class);

	@Autowired(required = false)
	private IInfinispanClusterService infinispanClusterService;
	
	@Autowired(required = false)
	private IEtcdClusterService etcdClusterService;

	private final ILock lock;
	private final IPriorityQueue priorityQueue;
	
	public ConsumerInfinispanAwareThread(String name, long startDelay, TaskQueue queue,
			ILock lock, IPriorityQueue priorityQueue) {
		super(name, startDelay, queue);
		this.lock = lock;
		this.priorityQueue = priorityQueue;
	}
	
	@Override
	public void run() {
		try {
			if (startDelay > 0) {
				Thread.sleep(startDelay);
			}
			/*
			 * condition: permits thread to finish gracefully (stop was
			 * signaled, last taken element had been consumed, we can
			 * stop without any other action)
			 */
			while (active && !Thread.currentThread().isInterrupted()) {
				// if there are tasks to consume, rateLimiter is not limiting due to task consumption's duration
				// this allow to have a chance to read working flag when there is no job to be done.
				rateLimiter.acquire();
				final AtomicLong taskId = new AtomicLong();
				try {
					ILockRequest lockRequest = LockRequest.with(null, lock, priorityQueue);
					// check role / lock with priority and perform
					final Runnable runnable = () -> {
						try {
							Long queuedTaskHolderId = queue.poll(100, TimeUnit.MILLISECONDS);
							
							if (queuedTaskHolderId != null) {
								taskId.set(queuedTaskHolderId);
								setWorking(true);
								// if not active, we are about to stop, ignoring task
								if (active) {
									entityManagerUtils.openEntityManager();
									try {
										tryConsumeTask(queuedTaskHolderId);
									} finally {
										entityManagerUtils.closeEntityManager();
									}
								}
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						} finally {
							setWorking(false);
						}
					};
					if (isEtcdEnable()) {
						etcdClusterService.doWithLockPriority(lockRequest, runnable);
					} else {
						infinispanClusterService.doWithLockPriority(lockRequest, runnable);
					}
				} catch (ExecutionException e) {
					throw new IllegalStateException(e);
				} catch(RuntimeException e) {
					LOGGER.warn("Unknown Infinispan exception", e);
					// task has failed - trying again
					if (taskId.get() != 0) {
						queue.offer(taskId.get());
					}
				}
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean isEtcdEnable() {
		return etcdClusterService != null;
	}
}
