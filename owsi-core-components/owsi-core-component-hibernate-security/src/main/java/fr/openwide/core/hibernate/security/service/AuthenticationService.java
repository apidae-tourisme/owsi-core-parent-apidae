package fr.openwide.core.hibernate.security.service;

import java.util.Collection;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;

import fr.openwide.core.hibernate.business.generic.model.GenericEntity;
import fr.openwide.core.hibernate.security.business.person.model.CorePerson;

public interface AuthenticationService {

	String getUserName();
	
	boolean isLoggedIn();
	
	CorePerson getPerson();
	
	Collection<GrantedAuthority> getAuthorities();

	boolean hasSystemRole();
	
	boolean hasAdminRole();
	
	boolean hasAuthenticatedRole();
	
	boolean hasRole(String role);
	
	boolean hasPermission(GenericEntity<?, ?> entity, Permission permission);
	
	boolean isAnonymousAuthority(String grantedAuthoritySid);
	
	void signOut();
	
}
