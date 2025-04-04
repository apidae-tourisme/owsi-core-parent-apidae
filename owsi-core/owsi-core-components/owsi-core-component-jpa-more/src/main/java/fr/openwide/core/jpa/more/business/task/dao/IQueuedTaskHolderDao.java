package fr.openwide.core.jpa.more.business.task.dao;

import java.util.Date;
import java.util.List;

import fr.openwide.core.jpa.business.generic.dao.IGenericEntityDao;
import fr.openwide.core.jpa.more.business.task.model.QueuedTaskHolder;
import fr.openwide.core.jpa.more.business.task.util.TaskStatus;

public interface IQueuedTaskHolderDao extends IGenericEntityDao<Long, QueuedTaskHolder> {

	Long count(Date since, TaskStatus... statuses);
	
	Long count(TaskStatus... statuses);
	
	/**
	 * Récupère en base la tâche suivante à exécuter.
	 * 
	 * @param taskType type de tâche à récupérer
	 * 
	 * @return cette tâche, <code>null</code> si aucune tâche à exécuter.
	 */
	QueuedTaskHolder getNextTaskForExecution(String taskType);

	/**
	 * Renvoi aléatoirement l'une tâche considérée comme bloquée, c'est à dire dont le temps d'exécution est jugé
	 * trop long. La limite du temps d'exécution est fixée par la propriété 'sitra.tasks.executionTimeLimitInSeconds'
	 * dans la configuration.
	 * 
	 * @param taskType type de tâche à récupérer
	 * @param executionTimeLimitInSeconds le temps d'exécution au-delà duquel la tâche est considrée comme bloquée.
	 * 
	 * @return l'une des tâches bloquées, <code>null</code> si aucune tâche n'est bloquée.
	 */
	QueuedTaskHolder getStalledTask(String taskType, int executionTimeLimitInSeconds);
	
	List<QueuedTaskHolder> listConsumable();
	
	List<QueuedTaskHolder> listConsumable(String queueId, Integer limit);

	List<String> listTypes();
}
