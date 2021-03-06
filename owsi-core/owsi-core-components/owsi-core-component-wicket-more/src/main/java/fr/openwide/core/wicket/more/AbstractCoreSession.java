package fr.openwide.core.wicket.more;

import java.util.Collection;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.collect.Lists;

import fr.openwide.core.jpa.exception.SecurityServiceException;
import fr.openwide.core.jpa.exception.ServiceException;
import fr.openwide.core.jpa.security.business.authority.util.CoreAuthorityConstants;
import fr.openwide.core.jpa.security.business.person.model.GenericUser;
import fr.openwide.core.jpa.security.business.person.service.IGenericUserService;
import fr.openwide.core.jpa.security.config.spring.DefaultJpaSecurityConfig;
import fr.openwide.core.jpa.security.model.NamedPermission;
import fr.openwide.core.jpa.security.service.IAuthenticationService;
import fr.openwide.core.spring.property.service.IPropertyService;
import fr.openwide.core.wicket.more.link.descriptor.IPageLinkDescriptor;
import fr.openwide.core.wicket.more.model.threadsafe.SessionThreadSafeGenericEntityModel;

public abstract class AbstractCoreSession<U extends GenericUser<U, ?>> extends AuthenticatedWebSession {

	private static final long serialVersionUID = 2591467597835056981L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoreSession.class);
	
	private static final String REDIRECT_URL_ATTRIBUTE_NAME = "redirectUrl";
	
	private static final String REDIRECT_PAGE_LINK_DESCRIPTOR_ATTRIBUTE_NAME = "redirectPageLinkDescriptor";
	
	@SpringBean(name="personService")
	protected IGenericUserService<U> userService;
	
	@SpringBean(name="authenticationService")
	protected IAuthenticationService authenticationService;
	
	@SpringBean(name="authenticationManager")
	protected AuthenticationManager authenticationManager;
	
	@SpringBean(name="propertyService")
	protected IPropertyService propertyService;
	
	private final IModel<U> userModel = new SessionThreadSafeGenericEntityModel<Long, U>();
	
	private final IModel<Locale> localeModel = new IModel<Locale>() {
		private static final long serialVersionUID = -4356509005738585888L;
		
		@Override
		public Locale getObject() {
			return AbstractCoreSession.this.getLocale();
		}
		
		@Override
		public void setObject(Locale object) {
			AbstractCoreSession.this.setLocale(object);
		}
		
		@Override
		public void detach() {
			// Nothing to do
		}
	};
	
	private Roles roles = new Roles();
	
	private boolean rolesInitialized = false;
	
	private Collection<? extends Permission> permissions = Lists.newArrayList();
	
	private boolean permissionsInitialized = false;
	
	private boolean isSuperUser = false;
	
	private boolean isSuperUserInitialized = false;
	
	public AbstractCoreSession(Request request) {
		super(request);
		
		Injector.get().inject(this);
		
		// Override browser locale with mapped locale
		// setLocale process locale to map to one available locale
		setLocale(getLocale());
	}
	
	public static AbstractCoreSession<?> get() {
		return (AbstractCoreSession<?>) Session.get();
	}

	/**
	 * Attempts to authenticate a user that has provided the given username and
	 * password.
	 * 
	 * @param username
	 *            current username
	 * @param password
	 *            current password
	 * @return <code>true</code> if authentication succeeds, throws an exception if not
	 * 
	 * @throws BadCredentialsException if password doesn't match with username
	 * @throws UsernameNotFoundException if user name was not found
	 * @throws DisabledException if user was found but disabled
	 */
	@Override
	public boolean authenticate(String username, String password)
			throws BadCredentialsException, UsernameNotFoundException, DisabledException {
		doAuthenticate(username, password);
		
		doInitializeSession();
		
		return true;
	}
	
	protected Authentication doAuthenticate(String username, String password) {
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		return authentication;
	}
	
	protected void doInitializeSession() {
		U user = userService.getByUserName(authenticationService.getUserName());
		
		if (user == null) {
			throw new IllegalStateException("Unable to find the signed in user.");
		}
		
		userModel.setObject(user);
		
		try {
			if (user.getLastLoginDate() == null) {
				onFirstLogin(user);
			}
			
			userService.updateLastLoginDate(user);
			
			Locale locale = user.getLocale();
			if (locale != null) {
				setLocale(user.getLocale());
			} else {
				// si la personne ne poss??de pas de locale
				// alors on enregistre celle mise en place
				// automatiquement par le navigateur.
				userService.updateLocale(user, getLocale());
			}
		} catch (RuntimeException | ServiceException | SecurityServiceException e) {
			LOGGER.error(String.format("Unable to update the user information on sign in: %1$s", user), e);
		}
		
		Collection<? extends GrantedAuthority> authorities = authenticationService.getAuthorities();
		roles = new Roles();
		for (GrantedAuthority authority : authorities) {
			roles.add(authority.getAuthority());
		}
		rolesInitialized = true;
		
		permissions = authenticationService.getPermissions();
		permissionsInitialized = true;
		
		isSuperUser = authenticationService.isSuperUser();
		isSuperUserInitialized = true;
	}
	
	protected void onFirstLogin(U user) {
	}
	
	public IModel<U> getUserModel() {
		return userModel;
	}

	/**
	 * @return the currently logged in user, or null when no user is logged in
	 */
	public String getUserName() {
		String userName = null;
		if (isSignedIn()) {
			userName = userModel.getObject().getUserName();
		}
		return userName;
	}

	public U getUser() {
		U person = null;

		if (isSignedIn()) {
			person = userModel.getObject();
		}

		return person;
	}

	/**
	 * Returns the current user roles.
	 * 
	 * @return current user roles
	 */
	@Override
	public Roles getRoles() {
		if (!rolesInitialized) {
			Collection<? extends GrantedAuthority> authorities = authenticationService.getAuthorities();
			for (GrantedAuthority authority : authorities) {
				roles.add(authority.getAuthority());
			}
			rolesInitialized = true;
		}
		return roles;
	}
	
	public boolean hasRole(String authority) {
		return getRoles().contains(authority);
	}

	public boolean hasRoleAdmin() {
		return hasRole(CoreAuthorityConstants.ROLE_ADMIN);
	}
	
	public boolean hasRoleAuthenticated() {
		return hasRole(CoreAuthorityConstants.ROLE_AUTHENTICATED);
	}
	
	public boolean hasRoleSystem() {
		return hasRole(CoreAuthorityConstants.ROLE_SYSTEM);
	}
	
	public boolean hasRoleAnonymous() {
		return hasRole(CoreAuthorityConstants.ROLE_ANONYMOUS);
	}
	
	protected Collection<? extends Permission> getPermissions() {
		if (!permissionsInitialized) {
			permissions = authenticationService.getPermissions();
			permissionsInitialized = true;
		}
		return permissions;
	}
	
	public boolean hasPermission(Permission permission) {
		if (isSuperUser()) {
			return true;
		}
		
		return getPermissions().contains(permission);
	}
	
	protected boolean isSuperUser() {
		if (!isSuperUserInitialized) {
			isSuperUser = authenticationService.isSuperUser();
			isSuperUserInitialized = true;
		}
		return isSuperUser;
	}

	/**
	 * Sign out the user. If you want to completely invalidate the session, call invalidate() instead.
	 * After a signout, you should redirect the browser to the home or sign in page.
	 */
	@Override
	public void signOut() {
		signOutWithoutCleaningUpRedirectUrl();
		
		removeAttribute(REDIRECT_URL_ATTRIBUTE_NAME);
	}
	
	/**
	 * @deprecated Only useful when using OWSI-Core's redirection mechanism, which is deprecated.
	 * 
	 * @see {@link #registerRedirectUrl(String)} for information about alternative mechanisms.
	 */
	@Deprecated
	public void signOutWithoutCleaningUpRedirectUrl() {
		userModel.setObject(null);
		roles = new Roles();
		rolesInitialized = false;
		permissions = Lists.newArrayList();
		permissionsInitialized = false;
		
		authenticationService.signOut();
		
		super.signOut();
	}
	
	/**
	 * @deprecated This was OWSI-Core's own redirection mechanism, which is now deprecated in favor of more standard
	 * mechanisms.
	 * You may use instead:
	 * <ul>
	 *  <li>Wicket's
	 *  {@link RestartResponseAtInterceptPageException} mechanism
	 *  ({@link Component#redirectToInterceptPage(org.apache.wicket.Page)},
	 *  {@link Component#continueToOriginalDestination()}), if redirecting within the same session (without login/logout
	 *  during the redirection process).
	 *  <li>Spring Security's saved requests mechanisms, triggered by an {@link AccessDeniedException}, if redirecting
	 *  after an authentication/authorization error. Beware that most cases are already handled in OWSI-Core through the
	 *  {@link CoreDefaultExceptionMapper}, so you normally shouldn't have to do this.
	 *  <li>Or you own implementation with an URL as a page parameter, for the most specific needs.
	 * </ul>
	 */
	@Deprecated
	public void registerRedirectUrl(String url) {
		// le bind() est obligatoire pour demander ?? wicket de persister la session
		// si on ne le fait pas, la session poss??de comme dur??e de vie le temps de
		// la requ??te.
		if (isTemporary()) {
			bind();
		}
		
		setAttribute(REDIRECT_URL_ATTRIBUTE_NAME, url);
	}
	
	/**
	 * @deprecated This was OWSI-Core's own redirection mechanism, which is now deprecated in favor of more standard
	 * mechanisms.
	 * @see {@link #registerRedirectUrl(String)} for information about alternative mechanisms.
	 */
	public String getRedirectUrl() {
		return (String) getAttribute(REDIRECT_URL_ATTRIBUTE_NAME);
	}
	
	/**
	 * @deprecated This was OWSI-Core's own redirection mechanism, which is now deprecated in favor of more standard
	 * mechanisms.
	 * @see {@link #registerRedirectUrl(String)} for information about alternative mechanisms.
	 */
	public String consumeRedirectUrl() {
		String redirectUrl = getRedirectUrl();
		removeAttribute(REDIRECT_URL_ATTRIBUTE_NAME);
		return redirectUrl;
	}
	
	/**
	 * @deprecated This was OWSI-Core's own redirection mechanism, which is now deprecated in favor of more standard
	 * mechanisms.
	 * @see {@link #registerRedirectUrl(String)} for information about alternative mechanisms.
	 */
	public void registerRedirectPageLinkDescriptor(IPageLinkDescriptor pageLinkDescriptor) {
		// le bind() est obligatoire pour demander ?? wicket de persister la session
		// si on ne le fait pas, la session poss??de comme dur??e de vie le temps de
		// la requ??te.
		if (isTemporary()) {
			bind();
		}
		
		setAttribute(REDIRECT_PAGE_LINK_DESCRIPTOR_ATTRIBUTE_NAME, pageLinkDescriptor);
	}
	
	/**
	 * @deprecated This was OWSI-Core's own redirection mechanism, which is now deprecated in favor of more standard
	 * mechanisms.
	 * @see {@link #registerRedirectUrl(String)} for information about alternative mechanisms.
	 */
	public IPageLinkDescriptor getRedirectPageLinkDescriptor() {
		IPageLinkDescriptor pageLinkDescriptor = (IPageLinkDescriptor) getAttribute(REDIRECT_PAGE_LINK_DESCRIPTOR_ATTRIBUTE_NAME);
		removeAttribute(REDIRECT_PAGE_LINK_DESCRIPTOR_ATTRIBUTE_NAME);
		return pageLinkDescriptor;
	}
	
	/**
	 * <p>Override to provide locale mapping to available application locales.</p>
	 * @return 
	 */
	@Override
	public Session setLocale(Locale locale) {
		return super.setLocale(propertyService.toAvailableLocale(locale));
	}
	
	public IModel<Locale> getLocaleModel() {
		return localeModel;
	}
	
	@Override
	public void detach() {
		super.detach();
		
		userModel.detach();
		localeModel.detach();
	}
	
	@Override
	public void internalDetach() {
		super.internalDetach();
		
		userModel.detach();
		localeModel.detach();
	}

	@SpringBean
	private DefaultJpaSecurityConfig defaultJpaSecurityConfig;

	@SpringBean(name = "userDetailsService")
	private UserDetailsService userDetailsService;

	/**
	 * Utilis?? pour garder l'authentification de l'utilisateur lorsqu'il se connecte en tant qu'un autre utilisateur.
	 */
	private Authentication originalAuthentication = null;

	public Authentication getOriginalAuthentication() {
		return originalAuthentication;
	}

	public boolean hasSignInAsPermissions(U utilisateurConnecte, U utilisateurCible) {
		return authenticationService.hasPermission(NamedPermission.ADMIN_SIGN_IN_AS);
	}

	/**
	 * @see AbstractCoreSession#authenticate(String, String)
	 */
	public void signInAs(String username) throws UsernameNotFoundException {
		// on charge l'utilisateur
		// on le passe dans une m??thode surchargeable -> impl??mentation par d??faut ?? faire
		// Sitra -> revoir l'impl??mentation par d??faut
		if (!hasSignInAsPermissions(getUser(), userService.getByUserName(username))) {
			throw new SecurityException("L'utilisateur n'a pas les permissions n??cessaires");
		}
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		RunAsUserToken token = new RunAsUserToken(defaultJpaSecurityConfig.getRunAsKey(),
				userDetails, "runAs", userDetails.getAuthorities(), null);
		
		// On garde l'authentification de l'utilisateur pour pouvoir lui proposer de se reconnecter.
		Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(previousAuthentication instanceof AnonymousAuthenticationToken)) {
			originalAuthentication = previousAuthentication;
		}
		
		signOut();
		
		Authentication authentication = authenticationManager.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		doInitializeSession();
		bind();
		signIn(true);
	}

	public void signInAsMe() throws BadCredentialsException, SecurityException {
		if (originalAuthentication == null) {
			throw new BadCredentialsException("Pas d'authentification originelle");
		}
		
		SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
		doInitializeSession();
		bind();
		signIn(true);
		originalAuthentication = null;
	}

}
