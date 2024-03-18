package fr.openwide.core.jpa.more.business.task.service;

import org.springframework.context.annotation.Configuration;

import fr.openwide.core.jpa.more.business.task.model.TaskTypesRegistry;

/**
 * Allow applications to contribute task types. Used for administration UI. Implements this interface on
 * @{@link Bean} or @{@link Configuration} to contribute your task types.
 */
public interface ITaskTypeConfigurer {

	void configureTaskType(TaskTypesRegistry registry);

}
