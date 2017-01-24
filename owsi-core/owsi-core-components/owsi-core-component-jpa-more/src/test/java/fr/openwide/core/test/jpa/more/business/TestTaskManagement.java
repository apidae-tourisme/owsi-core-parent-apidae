package fr.openwide.core.test.jpa.more.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;

import fr.openwide.core.jpa.business.generic.service.IEntityService;
import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.more.business.task.model.AbstractTask;
import fr.openwide.core.jpa.more.business.task.model.QueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.model.TaskExecutionResult;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderManager;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderService;
import fr.openwide.core.jpa.more.business.task.util.TaskResult;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;
import fr.openwide.core.test.jpa.more.business.task.config.TestTaskManagementConfig;

@ContextConfiguration(classes = TestTaskManagementConfig.class)
public class TestTaskManagement extends AbstractJpaMoreTestCase {

	@Autowired
	private IEntityService entityService;
	
	@Autowired
	private IQueuedTaskHolderManager manager;
	
	@Autowired
	private IQueuedTaskHolderService taskHolderService;

	private TransactionTemplate transactionTemplate;
	
	/**
	 * A utility used to check that a given task has been correctly executed.
	 * <p>Designed to always reference the same value, even having been serialized with Jackson.
	 */
	protected static class StaticValueAccessor<T> implements Supplier<T>, Serializable {
		private static final long serialVersionUID = 1L;
		
		protected static final ConcurrentMap<Integer, Object> values = Maps.newConcurrentMap();
		protected static volatile int idCounter = 0;
		
		private int id;
		
		public StaticValueAccessor() {
			this.id = ++idCounter;
		}
		
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T get() {
			return (T) values.get(id); 
		}

		public void set(T value) {
			values.put(id, value);
		}
	}
	
	private static abstract class AbstractTestTask extends AbstractTask {
		private static final long serialVersionUID = 1L;
		public AbstractTestTask() {
			super("task", "type", new Date());
		}
	}

	public static class SimpleTestTask<T> extends AbstractTestTask {
		private static final long serialVersionUID = 1L;
		
		private StaticValueAccessor<T> valueAccessor;

		private T expectedValue;

		@JsonIgnoreProperties("stackTrace")
		private TaskExecutionResult expectedResult;
		
		private int timeToWaitMs = 0;
		
		protected SimpleTestTask() {
		}
		
		public SimpleTestTask(StaticValueAccessor<T> valueAccessor, T expectedValue, TaskExecutionResult expectedResult) {
			super();
			this.valueAccessor = valueAccessor;
			this.expectedValue = expectedValue;
			this.expectedResult = expectedResult;
		}
		
		@Override
		protected TaskExecutionResult doTask() throws Exception {
			if (timeToWaitMs != 0) {
				Thread.sleep(timeToWaitMs);
			}
			valueAccessor.set(expectedValue);
			return expectedResult;
		}

		public StaticValueAccessor<T> getValueAccessor() {
			return valueAccessor;
		}

		public void setValueAccessor(StaticValueAccessor<T> valueAccessor) {
			this.valueAccessor = valueAccessor;
		}

		public T getExpectedValue() {
			return expectedValue;
		}

		public void setExpectedValue(T expectedValue) {
			this.expectedValue = expectedValue;
		}

		public TaskExecutionResult getExpectedResult() {
			return expectedResult;
		}

		public void setExpectedResult(TaskExecutionResult expectedResult) {
			this.expectedResult = expectedResult;
		}

		public int getTimeToWaitMs() {
			return timeToWaitMs;
		}

		public void setTimeToWaitMs(int timeToWaitMs) {
			this.timeToWaitMs = timeToWaitMs;
		}
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	protected void cleanAll() throws ServiceException, SecurityServiceException {
		cleanEntities(taskHolderService);
		super.cleanAll();
	}
	
	protected void waitTaskConsumption() {
		waitTaskConsumption(false, true);
	}
	
	protected void waitTaskConsumption(boolean returnIfStopped, boolean waitForRunning) {
		RateLimiter rateLimiter = RateLimiter.create(1);
		int tryCount = 0;
		while (true) {
			tryCount++;
			rateLimiter.acquire();
			
			if (
					// if returnIfStopped == true, we consider that wait is done
					(returnIfStopped || manager.isActive())
					&& manager.getNumberOfWaitingTasks() == 0
					// if we don't wait for running, ignore manager.getNumberOfRunningTasks()
					&& (!waitForRunning || manager.getNumberOfRunningTasks() == 0)) {
				break;
			}
			
			if (tryCount > 10) {
				throw new IllegalStateException(MessageFormat.format("Task queue not empty after {0} tries.", tryCount));
			}
		}
	}
	
	@Test
	public void simple() throws Exception {
		final StaticValueAccessor<String> result = new StaticValueAccessor<>();
		final StaticValueAccessor<Long> taskHolderId = new StaticValueAccessor<>();
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				try {
					QueuedTaskHolder taskHolder = manager.submit(
							new SimpleTestTask<>(result, "success", TaskExecutionResult.completed())
					);
					taskHolderId.set(taskHolder.getId());
				} catch (ServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});

		entityService.flush();
		entityService.clear();
		
		waitTaskConsumption();
		
		QueuedTaskHolder taskHolder = taskHolderService.getById(taskHolderId.get());
		assertEquals(TaskStatus.COMPLETED, taskHolder.getStatus());
		assertEquals(TaskResult.SUCCESS, taskHolder.getResult());
		assertEquals("success", result.get());
	}
	
	@Test
	public void noTransaction() throws Exception {
		final StaticValueAccessor<String> result = new StaticValueAccessor<>();
		QueuedTaskHolder taskHolder = manager.submit(
				new SimpleTestTask<>(result, "success", TaskExecutionResult.completed())
		);

		entityService.flush();
		entityService.clear();
		
		waitTaskConsumption();
		
		taskHolder = taskHolderService.getById(taskHolder.getId());
		assertEquals(TaskStatus.COMPLETED, taskHolder.getStatus());
		assertEquals(TaskResult.SUCCESS, taskHolder.getResult());
		assertEquals("success", result.get());
	}
	
	@Test
	public void submitInLongTransaction() throws Exception {
		final StaticValueAccessor<String> result = new StaticValueAccessor<>();
		final StaticValueAccessor<String> result2 = new StaticValueAccessor<>();
		final StaticValueAccessor<Long> taskHolderId = new StaticValueAccessor<>();
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				transactionTemplate.execute(new TransactionCallbackWithoutResult() {
					@Override
					public void doInTransactionWithoutResult(TransactionStatus status) {
						try {
							QueuedTaskHolder taskHolder = manager.submit(
									new SimpleTestTask<>(result, "success", TaskExecutionResult.completed())
							);
							
							taskHolderId.set(taskHolder.getId());
							
							// wait for task 2 to start and complete
							RateLimiter rateLimiter = RateLimiter.create(0.5);
							int tryCount = 0;
							while (true) {
								tryCount++;
								rateLimiter.acquire();
								
								if (result2.get() != null && result2.get().equals("success")) {
									break;
								}
								
								if (tryCount > 10) {
									throw new IllegalStateException(MessageFormat.format("Task 2 not found done after {0} tries.", tryCount));
								}
							}
							
							// Check that the task has not been consumed during this transaction (which could be aborted)
							// and that task 2 is allowed to be done
							assertNull(result.get());
						} catch (ServiceException e) {
							throw new IllegalStateException(e);
						}
					}
				});
			}
		};
		// thread needed so that second task can be run and completed before the above transaction
		Thread t = new Thread(runnable);
		t.start();
		
		// push another task
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				try {
					manager.submit(
							new SimpleTestTask<>(result2, "success", TaskExecutionResult.completed())
					);
				} catch (ServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		
		entityService.flush();
		entityService.clear();
		
		waitTaskConsumption();
		
		QueuedTaskHolder taskHolder = taskHolderService.getById(taskHolderId.get());
		assertEquals(TaskStatus.COMPLETED, taskHolder.getStatus());
		assertEquals(TaskResult.SUCCESS, taskHolder.getResult());
		assertEquals("success", result.get());
	}
	
	public static class SelfInterruptingTask<T> extends SimpleTestTask<T> {
		private static final long serialVersionUID = 1L;
		
		@Autowired
		private IQueuedTaskHolderManager manager;

		protected SelfInterruptingTask() {
			super();
		}

		public SelfInterruptingTask(StaticValueAccessor<T> valueAccessor, T expectedValue,
				TaskExecutionResult expectedResult) {
			super(valueAccessor, expectedValue, expectedResult);
		}
		
		@Override
		protected TaskExecutionResult doTask() throws Exception {
			// Stop the manager, then wait for it to interrupt us (waiting duration specified through setTimeToWait())
			manager.stop();
			return super.doTask();
		}
	}
	
	@Test
	public void interrupt() throws Exception {
		final StaticValueAccessor<String> result = new StaticValueAccessor<>();
		final StaticValueAccessor<Long> taskHolderId = new StaticValueAccessor<>();
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				try {
					// This task will stop the manager during its execution
					SelfInterruptingTask<String> testTask =
							new SelfInterruptingTask<>(result, "success", TaskExecutionResult.completed());
					// we want to force an interruption
					testTask.setTimeToWaitMs(2000);
					QueuedTaskHolder taskHolder = manager.submit(testTask);
					taskHolderId.set(taskHolder.getId());
				} catch (ServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		
		entityService.flush();
		entityService.clear();
		
		waitTaskConsumption(true, true);
		
		QueuedTaskHolder taskHolder = taskHolderService.getById(taskHolderId.get());
		assertEquals(TaskStatus.INTERRUPTED, taskHolder.getStatus());
		assertEquals(TaskResult.FATAL, taskHolder.getResult());
		assertNull(result.get());
	}

}
