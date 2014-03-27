package fr.openwide.jpa.example.business.person.service;

import fr.openwide.core.jpa.business.generic.service.IGenericEntityService;
import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.jpa.example.business.person.model.Person;
import fr.openwide.jpa.example.business.project.model.Project;

public interface PersonService extends IGenericEntityService<Long, Person> {
	
	void addProject(Person person, Project project) throws ServiceException, SecurityServiceException;
}