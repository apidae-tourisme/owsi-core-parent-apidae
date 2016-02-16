package fr.openwide.core.jpa.more.business.history.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import fr.openwide.core.jpa.business.generic.service.ITransactionScopeIndependantRunnerService;
import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.more.util.transaction.model.ITransactionSynchronizationBeforeCommitTask;

public class FactoredHistoryLogBeforeCommitWithDifferencesTask implements ITransactionSynchronizationBeforeCommitTask {
	
	@Autowired
	private ITransactionScopeIndependantRunnerService transactionScopeIndependantRunnerService;
	
	private final Set<HistoryLogBeforeCommitWithDifferencesTask<?, ?, ?, ?, ?>> tasks;

	public FactoredHistoryLogBeforeCommitWithDifferencesTask(Set<HistoryLogBeforeCommitWithDifferencesTask<?, ?, ?, ?, ?>> tasks) {
		super();
		this.tasks = tasks;
	}

	/**
	 * @return true, because this task requires its parameters to be still attached to the session when it executes.
	 */
	@Override
	public boolean shouldRunBeforeClear() {
		return true;
	}

	@Override
	public void run() throws Exception {
		final List<HistoryLogRunner<?>> runners = Lists.newArrayList();
		for (HistoryLogBeforeCommitWithDifferencesTask<?, ?, ?, ?, ?> task : tasks) {
			runners.add(new HistoryLogRunner<>(task));
		}

		for (HistoryLogRunner<?> runner : runners) {
			runner.prepareRetrieval();
		}
		
		/*
		 * Here lies the only interest of this class: executing all the objects fetches in one transaction, even if there
		 * are hundreds.
		 * It allows to take benefit from Hibernate's batch fetching (as we get all the proxies at first, then we
		 * initialize them).
		 */
		transactionScopeIndependantRunnerService.run(true, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (HistoryLogRunner<?> runner : runners) {
					runner.retrieveReference();
				}
				for (HistoryLogRunner<?> runner : runners) {
					runner.initializeReference();
				}
				return null;
			}
		});

		for (HistoryLogRunner<?> runner : runners) {
			runner.log();
		}
	}
	
	private static class HistoryLogRunner<T> {
		private final HistoryLogBeforeCommitWithDifferencesTask<T, ?, ?, ?, ?> task;
		private Callable<T> referenceProvider;
		private T reference;
		
		public HistoryLogRunner(HistoryLogBeforeCommitWithDifferencesTask<T, ?, ?, ?, ?> task) {
			super();
			this.task = task;
		}

		public void prepareRetrieval() {
			this.referenceProvider = task.getDifferenceGenerator().getReferenceProvider(task.getMainObject());
		}

		public void retrieveReference() {
			try {
				this.reference = referenceProvider.call();
			} catch (Exception e) {
				throw new IllegalStateException("Error retrieving a reference object for a diff", e);
			}
		}
		
		public void initializeReference() {
			try {
				task.getDifferenceGenerator().initializeReference(reference);
			} catch (Exception e) {
				throw new IllegalStateException("Error initializing a reference object for a diff", e);
			}
		}
		
		public void log() throws ServiceException, SecurityServiceException {
			task.logNow(reference);
		}
	}

}