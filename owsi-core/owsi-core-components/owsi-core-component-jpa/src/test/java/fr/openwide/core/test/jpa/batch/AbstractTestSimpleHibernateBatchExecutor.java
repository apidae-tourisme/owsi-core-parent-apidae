package fr.openwide.core.test.jpa.batch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.querydsl.jpa.impl.JPAQuery;

import fr.openwide.core.commons.util.functional.Joiners;
import fr.openwide.core.jpa.batch.executor.SimpleHibernateBatchExecutor;
import fr.openwide.core.jpa.batch.runnable.ReadOnlyBatchRunnable;
import fr.openwide.core.jpa.batch.runnable.ReadWriteBatchRunnable;
import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.query.IQuery;
import fr.openwide.core.jpa.query.Queries;
import fr.openwide.core.test.business.person.model.Person;
import fr.openwide.core.test.business.person.model.QPerson;

public abstract class AbstractTestSimpleHibernateBatchExecutor extends AbstractTestHibernateBatchExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestSimpleHibernateBatchExecutor.class);
	
	protected abstract SimpleHibernateBatchExecutor newSimpleHibernateBatchExecutor();
	
	@Test
	public void readWrite() {
		List<Long> toExecute = Lists.newArrayList(personIds);
		
		final List<Long> executed = Lists.newArrayList();
		
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		executor.run(Person.class, toExecute, new ReadWriteBatchRunnable<Person>() {
			@Override
			public void executeUnit(Person unit) {
				LOGGER.warn("Executing: " + unit.getDisplayName());
				unit.setLastName(NEW_LASTNAME_VALUE);
				try {
					personService.update(unit);
					executed.add(unit.getId());
				} catch (ServiceException | SecurityServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});

		assertEquals(toExecute, executed);
		assertAllPersonsNamed(NEW_LASTNAME_VALUE);
	}
	
	@Test
	public void readWriteInsideTransaction() {
		writeRequiredTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				readWrite();
			}
		});
		assertAllPersonsNamed(NEW_LASTNAME_VALUE);
	}
	
	@Test
	public void readOnly() {
		List<Long> toExecute = Lists.newArrayList(personIds);
		
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);

		executor.run(Person.class, toExecute, new ReadOnlyBatchRunnable<Person>() {
			@Override
			public void executeUnit(Person unit) {
				LOGGER.warn("Executing: " + unit.getDisplayName());
				unit.setLastName(NEW_LASTNAME_VALUE);
				try {
					personService.update(unit);
				} catch (ServiceException | SecurityServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		
		assertNoPersonNamed(NEW_LASTNAME_VALUE);
	}
	
	@Test
	public void readOnlyInsideTransaction() {
		writeRequiredTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				readOnly();
			}
		});
		assertNoPersonNamed(NEW_LASTNAME_VALUE);
	}

	@Test
	public void customNonConsumingQuery() {
		List<Long> toExecute = Lists.newArrayList(Iterables.skip(personIds, 50));
		Collections.sort(toExecute);
		
		IQuery<Person> query = Queries.fromQueryDsl(
				new JPAQuery<Person>(getEntityManager())
				.from(QPerson.person)
				.where(QPerson.person.id.in(toExecute))
				.orderBy(QPerson.person.id.desc())
		);
		List<Long> expectedExecuted = Lists.newArrayList(toExecute);
		Collections.sort(expectedExecuted, Ordering.natural().reverse());
		
		final List<Long> executed = Lists.newArrayList();
		
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		executor.runNonConsuming("Person query", query, new ReadOnlyBatchRunnable<Person>() {
			@Override
			public void executeUnit(Person unit) {
				LOGGER.warn("Executing: " + unit.getDisplayName());
				executed.add(unit.getId());
			}
		});
		
		assertEquals(expectedExecuted, executed);
	}
	
	@Test
	public void customConsumingQuery() {
		List<Long> toExecute = Lists.newArrayList(personIds);
		Collections.sort(toExecute);
		
		IQuery<Person> query = Queries.fromQueryDsl(
				new JPAQuery<Person>(getEntityManager())
				.from(QPerson.person)
				.where(QPerson.person.lastName.like("Lastname%"))
				.orderBy(QPerson.person.id.desc())
		);
		List<Long> expectedExecuted = Lists.newArrayList(toExecute);
		Collections.sort(expectedExecuted, Ordering.natural().reverse());
		
		final List<Long> executed = Lists.newArrayList();
		
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		executor.runConsuming("Person query", query, new ReadWriteBatchRunnable<Person>() {
			@Override
			public void executeUnit(Person unit) {
				LOGGER.warn("Executing: " + unit.getDisplayName());
				
				/* Remove the "Lastname" prefix, which "consumes" this element
				 * (e.g. it removes this element from the query's results)
				 */
				unit.setLastName(NEW_LASTNAME_VALUE);
				try {
					personService.update(unit);
					executed.add(unit.getId());
				} catch (ServiceException | SecurityServiceException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		
		assertEquals(expectedExecuted, executed);
	}
	
	private static class SequentialFailingRunnable<T> extends PartitionCountingRunnable<T> {
		@Override
		public void preExecutePartition(List<T> partition) {
			LOGGER.warn("Executing partition: " + Joiner.on(",").join(partition));
		}
		
		@Override
		public void executePartition(List<T> partition, int partitionIndex) {
			super.executePartition(partition, partitionIndex);
			switch (partitionIndex) {
			case 0: // First executePartition: succeed
				LOGGER.warn("executePartition#{}: Succeeding", partitionIndex);
				break;
			case 1: // Second executePartition: fail
				LOGGER.warn("executePartition#{}: Failing", partitionIndex);
				throw new TestBatchException1();
			default: // Should not happen
				Assert.fail();
			}
		}
	}
	
	@Test
	public void preExecuteErrorDefaultBehavior() {
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		
		Exception runException = null;
		PreExecuteFailingRunnable<Person> runnable = new PreExecuteFailingRunnable<>();
		try {
			executor.run(Person.class, personIds, runnable);
		} catch (Exception e) {
			runException = e;
		}

		assertThat(runException, instanceOf(IllegalStateException.class));
		assertThat(runException.getCause(), instanceOf(ExecutionException.class));
		assertThat(runException.getCause().getCause(), instanceOf(TestBatchException1.class));
		assertEquals(0, runnable.getExecutedPartitionCount());
	}
	
	@Test
	public void preExecuteErrorCustomBehavior() {
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		
		Exception runException = null;
		PreExecuteFailingRunnable<Person> runnable = new PreExecuteFailingRunnable<Person>() {
			@Override
			public void onError(ExecutionException exception) {
				throw new TestBatchException2(exception);
			}
		};
		try {
			executor.run(Person.class, personIds, runnable);
		} catch (Exception e) {
			runException = e;
		}
		
		assertThat(runException, instanceOf(TestBatchException2.class));
		assertThat(runException.getCause(), instanceOf(ExecutionException.class));
		assertThat(runException.getCause().getCause(), instanceOf(TestBatchException1.class));
		assertEquals(0, runnable.getExecutedPartitionCount());
	}
	
	@Test
	public void executePartitionErrorDefaultBehavior() {
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		
		Exception runException = null;
		SequentialFailingRunnable<Person> runnable = new SequentialFailingRunnable<>();
		try {
			executor.run(Person.class, personIds, runnable);
		} catch (Exception e) {
			runException = e;
		}

		assertThat(runException, instanceOf(IllegalStateException.class));
		assertThat(runException.getCause(), instanceOf(ExecutionException.class));
		assertThat(runException.getCause().getCause(), instanceOf(TestBatchException1.class));
		assertEquals(2, runnable.getExecutedPartitionCount());
	}
	
	@Test
	public void executePartitionErrorCustomBehavior() {
		SimpleHibernateBatchExecutor executor = newSimpleHibernateBatchExecutor();
		executor.batchSize(10);
		
		Exception runException = null;
		SequentialFailingRunnable<Person> runnable = new SequentialFailingRunnable<Person>() {
			@Override
			public void onError(ExecutionException exception) {
				throw new TestBatchException2(exception);
			}
		};
		try {
			executor.run(Person.class, personIds, runnable);
		} catch (Exception e) {
			runException = e;
		}
		
		assertThat(runException, instanceOf(TestBatchException2.class));
		assertThat(runException.getCause(), instanceOf(ExecutionException.class));
		assertThat(runException.getCause().getCause(), instanceOf(TestBatchException1.class));
		assertEquals(2, runnable.getExecutedPartitionCount());
	}
	
}
