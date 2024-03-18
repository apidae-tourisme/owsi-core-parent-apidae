package fr.openwide.core.jpa.more.business.task.model;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import fr.openwide.core.jpa.more.business.task.service.ITaskTypeConfigurer;

/**
 * Holder for possible task types. Used to build administration UI, as retrieving it dynamically from database
 * is computation intensive. See {@link ITaskTypeConfigurer} to configure types from your application.
 */
public class TaskTypesRegistry {

	private final Set<String> taskTypes = new TreeSet<>();

	public void addType(String type) {
		if (type == null) {
			throw new NullPointerException();
		}
		taskTypes.add(type);
	}

	public Set<String> getTypes() {
		return Collections.unmodifiableSet(taskTypes);
	}
}
