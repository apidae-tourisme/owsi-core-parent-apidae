package fr.openwide.core.jpa.more.config.spring;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

import fr.openwide.core.jpa.more.business.CoreJpaMoreBusinessPackage;
import fr.openwide.core.jpa.more.business.task.dao.IQueuedTaskHolderDao;
import fr.openwide.core.jpa.more.business.task.dao.QueuedTaskHolderDaoImpl;
import fr.openwide.core.jpa.more.business.task.model.IQueueId;
import fr.openwide.core.jpa.more.business.task.model.TaskTypesRegistry;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderManager;
import fr.openwide.core.jpa.more.business.task.service.IQueuedTaskHolderService;
import fr.openwide.core.jpa.more.business.task.service.ITaskTypeConfigurer;
import fr.openwide.core.jpa.more.business.task.service.QueuedTaskHolderManagerImpl;
import fr.openwide.core.jpa.more.business.task.service.QueuedTaskHolderServiceImpl;

@ComponentScan(basePackageClasses = { CoreJpaMoreBusinessPackage.class })
public abstract class AbstractTaskManagementConfig {
	
	public static final String OBJECT_MAPPER_BEAN_NAME = "queuedTaskHolderObjectMapper";

	@Bean(name = OBJECT_MAPPER_BEAN_NAME)
	public ObjectMapper queuedTaskHolderObjectMapper() {
		return new ObjectMapper().enableDefaultTyping(DefaultTyping.NON_FINAL);
	}

	@Bean
	public IQueuedTaskHolderDao queuedTaskHolderDao() {
		return new QueuedTaskHolderDaoImpl();
	}

	@Bean
	public TaskTypesRegistry taskTypesRegistry(List<ITaskTypeConfigurer> configurers) {
		TaskTypesRegistry registry = new TaskTypesRegistry();
		for (ITaskTypeConfigurer configurer : configurers) {
			configurer.configureTaskType(registry);
		}
		return registry;
	}

	@Bean
	public IQueuedTaskHolderService queuedTaskHolderService(IQueuedTaskHolderDao queuedTaskHolderDao, TaskTypesRegistry registry) {
		return new QueuedTaskHolderServiceImpl(queuedTaskHolderDao, registry);
	}
	
	@Bean
	public IQueuedTaskHolderManager queuedTaskHolderManager() {
		return new QueuedTaskHolderManagerImpl();
	}
	
	/**
	 * Must return all the {@link IQueueId queue IDs} that are valid in this application.
	 */
	@Bean
	public abstract Collection<? extends IQueueId> queueIds();
}
