package fr.openwide.core.etcd.action.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import fr.openwide.core.etcd.AbstractEtcdTest;
import fr.openwide.core.etcd.action.model.AbstractEtcdActionValue;
import fr.openwide.core.etcd.cache.model.action.ActionResultValue;
import fr.openwide.core.etcd.common.exception.EtcdServiceException;
import fr.openwide.core.etcd.common.service.EtcdClusterService;
import fr.openwide.core.etcd.common.service.IEtcdClusterService;
import fr.openwide.core.etcd.common.utils.EtcdClientClusterConfiguration;
import fr.openwide.core.etcd.common.utils.EtcdCommonClusterConfiguration;

public class EtcdActionServiceTest extends AbstractEtcdTest {

	private IEtcdClusterService etcdClusterService1;

	private static final String NODE_1 = "node_1";
	private static final String NODE_2 = "node_2";

	private IEtcdActionService etcdActionService;
	
	// Add a static field to track execution status
	private static final AtomicInteger actionExecuted = new AtomicInteger(0);

	@Before
	public void setUp() throws Exception {
		final EtcdCommonClusterConfiguration etcdConfigNode1 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(5).withLockTimeout(1).withNodeName(NODE_1).build();
		etcdClusterService1 = new EtcdClusterService(etcdConfigNode1);
		etcdClusterService1.init();

		etcdActionService = new EtcdActionService(etcdClusterService1, new EtcdClientClusterConfiguration(
				etcdConfigNode1, buildEctdClient(etcdConfigNode1)), null);
		// Reset the execution status before each test
		actionExecuted.set(0);
	}

	@Test
	public void resultLessAction() throws EtcdServiceException {
		assertThat(actionExecuted.get()).isZero();

		final TestAction testAction = new TestAction("node_1", false);
		final TestAction testAction2 = new TestAction("node_2", false);
		final TestAction testAction3 = new TestAction("node_3", false);

		etcdActionService.resultLessAction(testAction);
		etcdActionService.resultLessAction(testAction2);
		etcdActionService.resultLessAction(testAction3);

		final CompletableFuture<Void> stop = CompletableFuture.runAsync(() -> {
			// Stop cluster, to stop watcher and waiting end of task execution.
			etcdClusterService1.stop();
		}, // Wait that watch create trigger action
				CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
		stop.join();

		// Verify the action was executed
		assertThat(actionExecuted.get()).as("Only action for node 1 is executed").isEqualTo(1);
	}

	@Test
	public void resultLessActionWithAnotherNodes() throws Exception {

		assertThat(actionExecuted.get()).isZero();

		// adding another node
		final EtcdCommonClusterConfiguration etcdConfigNode2 = etcdConfigurationBuilderDefaultTestBuilder()
				.withLeaseTtl(5).withLockTimeout(1)
				.withNodeName(NODE_2).build();

		try (IEtcdClusterService etcdClusterService2 = new EtcdClusterService(etcdConfigNode2)) {
			etcdClusterService2.init();


			final TestAction testAction = new TestAction("node_1", false);
			final TestAction testAction2 = new TestAction("node_2", false);
			final TestAction testAction3 = new TestAction("node_3", false);
			etcdActionService.resultLessAction(testAction3);

			etcdActionService.resultLessAction(testAction);
			etcdActionService.resultLessAction(testAction2);

			final CompletableFuture<Void> stop = CompletableFuture.runAsync(() -> {
				// Stop cluster, to stop watcher and waiting end of task execution.
				etcdClusterService1.stop();
			}, // Wait that watch create trigger action
					CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
			stop.join();
		}

		// Verify the action was executed
		assertThat(actionExecuted.get()).as("Action for node_1 and node_2 are executed").isEqualTo(2);
	}

	@Test
	public void resultLessActionBroadcast() throws EtcdServiceException {
		assertThat(actionExecuted.get()).isZero();

		final TestAction testAction = new TestAction("node_1", true);
		final TestAction testAction2 = new TestAction("node_2", true);

		etcdActionService.resultLessAction(testAction);
		etcdActionService.resultLessAction(testAction2);

		final CompletableFuture<Void> stop = CompletableFuture.runAsync(() -> {
			// Stop cluster, to stop watcher and waiting end of task execution.
			etcdClusterService1.stop();
		}, // Wait that watch create trigger action
				CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
		stop.join();

		// Verify the action was executed
		assertThat(actionExecuted.get()).as("All action are executed because of broadcast config").isEqualTo(2);
	}

	public static class TestAction extends AbstractEtcdActionValue {

		private static final long serialVersionUID = 3579470843219510159L;

		public TestAction(String target, boolean broadcast) {
			super(broadcast, target);
		}

		@Override
		protected ActionResultValue doExecute() {
			EtcdActionServiceTest.actionExecuted.incrementAndGet();
			return ActionResultValue.from(true);
		}
	}
}
