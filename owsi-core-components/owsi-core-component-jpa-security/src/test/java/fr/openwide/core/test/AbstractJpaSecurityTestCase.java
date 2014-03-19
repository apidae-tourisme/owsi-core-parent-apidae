package fr.openwide.core.test;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.junit.AbstractTestCase;
import fr.openwide.core.jpa.security.business.authority.model.Authority;
import fr.openwide.core.jpa.security.business.authority.service.IAuthorityService;
import fr.openwide.core.jpa.security.business.authority.util.CoreAuthorityConstants;
import fr.openwide.core.jpa.security.business.person.model.IUser;
import fr.openwide.core.jpa.security.service.IAuthenticationService;
import fr.openwide.core.jpa.security.service.ISecurityService;
import fr.openwide.core.test.jpa.security.business.person.model.MockUser;
import fr.openwide.core.test.jpa.security.business.person.model.MockUserGroup;
import fr.openwide.core.test.jpa.security.business.person.service.IMockUserGroupService;
import fr.openwide.core.test.jpa.security.business.person.service.IMockUserService;
import fr.openwide.core.test.jpa.security.config.spring.JpaSecurityTestConfig;

@ContextConfiguration(classes = JpaSecurityTestConfig.class)
public abstract class AbstractJpaSecurityTestCase extends AbstractTestCase {

	public static final String DEFAULT_PASSWORD = "test";

	public static final String ROLE_GROUP_1 = "ROLE_GROUP_1";

	public static final String ROLE_GROUP_2 = "ROLE_GROUP_2";

	public static final String ROLE_GROUP_3 = "ROLE_GROUP_3";

	@Autowired
	protected IMockUserService mockPersonService;

	@Autowired
	protected IMockUserGroupService mockPersonGroupService;

	@Autowired
	protected IAuthorityService authorityService;

	@Autowired
	protected IAuthenticationService authenticationService;

	@Autowired
	protected ISecurityService securityService;

	@Autowired
	protected ProviderManager authenticationManager;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Before
	@Override
	public void init() throws ServiceException, SecurityServiceException {
		super.init();
		
		createAuthority(CoreAuthorityConstants.ROLE_SYSTEM);
		createAuthority(CoreAuthorityConstants.ROLE_ADMIN);
		createAuthority(CoreAuthorityConstants.ROLE_AUTHENTICATED);
		createAuthority(CoreAuthorityConstants.ROLE_ANONYMOUS);
		
		createAuthority(ROLE_GROUP_1);
		createAuthority(ROLE_GROUP_2);
		createAuthority(ROLE_GROUP_3);
	}
	
	@After
	@Override
	public void close() throws ServiceException, SecurityServiceException {
		super.close();
	}

	protected void cleanMockPersons() throws ServiceException, SecurityServiceException {
		List<MockUser> mockPersons = mockPersonService.list();
		for (MockUser person : mockPersons) {
			mockPersonService.delete(person);
		}
	}

	protected void cleanMockPersonGroups() throws ServiceException, SecurityServiceException {
		List<MockUserGroup> mockPersonGroups = mockPersonGroupService.list();
		for (MockUserGroup mockPersonGroup : mockPersonGroups) {
			mockPersonGroupService.delete(mockPersonGroup);
		}
	}

	protected void cleanAuthorities() throws ServiceException, SecurityServiceException {
		List<Authority> authorities = authorityService.list();
		for (Authority authority : authorities) {
			authorityService.delete(authority);
		}
	}

	@Override
	protected void cleanAll() throws ServiceException, SecurityServiceException {
		cleanMockPersons();
		cleanMockPersonGroups();
		cleanAuthorities();
	}

	protected MockUser createMockPerson(String userName, String firstName, String lastName) throws ServiceException, SecurityServiceException {
		return createMockPerson(userName, firstName, lastName, "test@example.com");
	}
	
	protected MockUser createMockPerson(String userName, String firstName, String lastName, String email) throws ServiceException, SecurityServiceException {
		MockUser person = new MockUser();
		person.setUserName(userName);
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setEmail(email);
		
		person.addAuthority(authorityService.getByName(CoreAuthorityConstants.ROLE_AUTHENTICATED));
		
		mockPersonService.save(person);
		mockPersonService.setPasswords(person, DEFAULT_PASSWORD);
		
		return person;
	}
	
	protected MockUserGroup createMockPersonGroup(String name) throws ServiceException, SecurityServiceException {
		MockUserGroup personGroup = new MockUserGroup();
		personGroup.setName(name);
		
		mockPersonGroupService.save(personGroup);
		
		return personGroup;
	}
	
	protected Authority createAuthority(String name) throws ServiceException, SecurityServiceException {
		Authority authority = new Authority();
		authority.setName(name);
		
		authorityService.save(authority);
		
		return authority;
	}
	
	protected void authenticateAs(IUser person) {
		authenticateAs(new UsernamePasswordAuthenticationToken(person.getUserName(), DEFAULT_PASSWORD));
	}
	
	protected void authenticateAs(UsernamePasswordAuthenticationToken authenticationToken) {
		Authentication authentication = authenticationManager.authenticate(authenticationToken);
		
		SecurityContext secureContext = new SecurityContextImpl();
		secureContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(secureContext);
	}
}