package fr.openwide.core.test.infinispan.util.process;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.test.infinispan.util.TestCacheManagerBuilder;
import fr.openwide.core.test.infinispan.util.listener.MonitorNotifyListener;

public class SimpleProcess implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProcess.class);

	private final String nodeName;

	private final String taskName;

	public SimpleProcess(String nodeName, String taskName) {
		super();
		this.nodeName = nodeName;
		this.taskName = taskName;
	}

	@Override
	public void run() {
		final EmbeddedCacheManager cacheManager = new TestCacheManagerBuilder(nodeName, taskName).build();
		cacheManager.addListener(new MonitorNotifyListener() {
			@ViewChanged
			public void onViewChanged(ViewChangedEvent viewChangedEvent) {
				LOGGER.debug("{}: view change -  {}", nodeName, viewChangedEvent.getNewMembers().size());
			}
		});
		cacheManager.start();
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Object object = new Object();
				// synchronized to become object owner
				LOGGER.debug("Start main loop");
				synchronized (object) {
					object.wait();
				}
			} catch (InterruptedException e) {
				LOGGER.warn("Thread interrupted while wait");
				Thread.currentThread().interrupt();
			}
		}
	}

	public static void main(String[] parameters) {
		if (parameters.length != 1 && parameters.length != 2) {
			System.err.println("Usage: <node name> [task.class.name]");
			System.exit(1);
		} else {
			String taskName = null;
			if (parameters.length == 2) {
				taskName = parameters[1];
			}
			new SimpleProcess(parameters[0], taskName).run();
		}
	}

}
