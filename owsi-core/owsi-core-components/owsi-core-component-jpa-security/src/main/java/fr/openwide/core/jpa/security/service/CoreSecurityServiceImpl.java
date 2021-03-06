package fr.openwide.core.jpa.security.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.intercept.RunAsImplAuthenticationProvider;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.openwide.core.jpa.security.business.authority.util.CoreAuthorityConstants;
import fr.openwide.core.jpa.security.business.person.model.IUser;
import fr.openwide.core.jpa.security.runas.RunAsSystemToken;
import fr.openwide.core.jpa.security.util.UserConstants;

public class CoreSecurityServiceImpl implements ISecurityService {

	public static final String SYSTEM_USER_NAME = "system";
	
	@Autowired
	protected UserDetailsService userDetailsService;

	@Autowired
	protected RunAsImplAuthenticationProvider runAsAuthenticationProvider;

	@Autowired
	protected ICorePermissionEvaluator permissionEvaluator;
	
	@Autowired
	protected RoleHierarchy roleHierarchy;

	@Autowired
	private IAuthenticationService authenticationService;
	
	@Override
	public boolean hasRole(Authentication authentication, String role) {
		if (authentication != null && role != null) {
			return authentication.getAuthorities().contains(new SimpleGrantedAuthority(role));
		}
		return false;
	}

	@Override
	public boolean hasRole(IUser person, String role) {
		if (person == null) {
			return false;
		}

		return hasRole(getAuthentication(person), role);
	}

	@Override
	public boolean hasSystemRole(Authentication authentication) {
		return hasRole(authentication, CoreAuthorityConstants.ROLE_SYSTEM);
	}

	@Override
	public boolean hasSystemRole(IUser person) {
		return hasRole(person, CoreAuthorityConstants.ROLE_SYSTEM);
	}

	@Override
	public boolean hasAdminRole(Authentication authentication) {
		return hasRole(authentication, CoreAuthorityConstants.ROLE_ADMIN);
	}

	@Override
	public boolean hasAdminRole(IUser person) {
		return hasRole(person, CoreAuthorityConstants.ROLE_ADMIN);
	}

	@Override
	public boolean hasAuthenticatedRole(Authentication authentication) {
		return hasRole(authentication, CoreAuthorityConstants.ROLE_AUTHENTICATED);
	}

	@Override
	public boolean hasAuthenticatedRole(IUser person) {
		return hasRole(person, CoreAuthorityConstants.ROLE_AUTHENTICATED);
	}

	@Override
	public boolean isAnonymousAuthority(String grantedAuthoritySid) {
		return CoreAuthorityConstants.ROLE_ANONYMOUS.equals(grantedAuthoritySid);
	}

	@Override
	public List<GrantedAuthority> getAuthorities(Authentication authentication) {
		if (authentication != null) {
			return new ArrayList<GrantedAuthority>(authentication.getAuthorities());
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<GrantedAuthority> getAuthorities(IUser person) {
		return getAuthorities(getAuthentication(person));
	}

	@Override
	public SecurityContext buildSecureContext(String userName) {
		SecurityContext secureContext = new SecurityContextImpl();
		secureContext.setAuthentication(getAuthentication(userName));

		return secureContext;
	}
	
	protected void authenticateAs(IUser user) {
		authenticateAs(user.getUserName());
	}

	protected void authenticateAs(String userName, String... additionalAuthorities) {
		clearAuthentication();

		UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

		Set<GrantedAuthority> authorities = Sets.newHashSet(userDetails.getAuthorities());
		if (additionalAuthorities != null) {
			for (String additionalAuthority : additionalAuthorities) {
				authorities.add(new SimpleGrantedAuthority(additionalAuthority));
			}
		}

		Authentication authentication = new RunAsUserToken(runAsAuthenticationProvider.getKey(), userDetails,
				UserConstants.NO_CREDENTIALS, authorities, UsernamePasswordAuthenticationToken.class);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	protected void authenticateAsSystem() {
		RunAsSystemToken runAsSystem = new RunAsSystemToken(runAsAuthenticationProvider.getKey(),
				UserConstants.SYSTEM_USER_NAME,
				roleHierarchy.getReachableGrantedAuthorities(Lists.newArrayList(new SimpleGrantedAuthority(CoreAuthorityConstants.ROLE_SYSTEM))));
		AuthenticationUtil.setAuthentication(runAsAuthenticationProvider.authenticate(runAsSystem));
	}

	@Override
	public void clearAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	/**
	 * Ex??cute une {@link Callable} en tant qu'utilisateur syst??me. Le
	 * contexte de s??curit?? est modifi?? au d??but de la t??che et r??tabli ?? la fin
	 * de la t??che.
	 * 
	 * @param task
	 *            un objet de type {@link Callable}
	 * 
	 * @return l'objet retourn?? par la m??thode {@link Callable#call()}
	 */
	@Override
	public <T> T runAsSystem(Callable<T> task) {
		Authentication originalAuthentication = AuthenticationUtil.getAuthentication();
		authenticateAsSystem();
		try {
			return task.call();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException(e);
		} finally {
			AuthenticationUtil.setAuthentication(originalAuthentication);
		}
	}

	@Override
	public <T> T runAs(Callable<T> task, String userName, String... additionalAuthorities) {
		Authentication originalAuthentication = AuthenticationUtil.getAuthentication();
		authenticateAs(userName, additionalAuthorities);
		try {
			return task.call();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException(e);
		} finally {
			AuthenticationUtil.setAuthentication(originalAuthentication);
		}
	}

	protected Authentication getAuthentication(IUser person) {
		return getAuthentication(person.getUserName());
	}

	protected Authentication getAuthentication(String userName) {
		// Si on demande la personne actuellement logg??e, on retourne directement l'authentication de la session
		Authentication authentication = authenticationService.getAuthentication();
		
		if (authentication != null && (authentication.getPrincipal() instanceof UserDetails)) {
			UserDetails details = (UserDetails)authentication.getPrincipal();
			
			if (Objects.equal(details.getUsername(), userName)) {
				return authentication;
			}
		}
		
		try {
			UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
			
			authentication = new RunAsUserToken(runAsAuthenticationProvider.getKey(), userDetails,
					"no-credentials", userDetails.getAuthorities(), UsernamePasswordAuthenticationToken.class);
			
			return authentication;
		} catch (DisabledException e) {
			return null;
		}
	}

	@Override
	public boolean hasPermission(Authentication authentication, Object securedObject,
			Permission requirePermission) {
		return permissionEvaluator.hasPermission(authentication, securedObject, requirePermission);
	}

	@Override
	public boolean hasPermission(IUser person, Object securedObject, Permission requirePermission) {
		return hasPermission(getAuthentication(person), securedObject, requirePermission);
	}

	@Override
	public boolean hasPermission(Authentication authentication, Permission permission) {
		return permissionEvaluator.hasPermission(authentication, permission);
	}
	
	@Override
	public boolean hasPermission(IUser person, Permission permission) {
		return hasPermission(getAuthentication(person), permission);
	}
	
	@Override
	public Collection<? extends Permission> getPermissions(Authentication authentication) {
		return permissionEvaluator.getPermissions(authentication);
	}
	
	@Override
	public boolean isSuperUser(Authentication authentication) {
		return permissionEvaluator.isSuperUser(authentication);
	}
	
}