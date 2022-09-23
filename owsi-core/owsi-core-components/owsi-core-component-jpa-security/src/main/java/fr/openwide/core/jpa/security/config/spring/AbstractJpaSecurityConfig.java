package fr.openwide.core.jpa.security.config.spring;

import static fr.openwide.core.spring.property.SpringSecurityPropertyIds.PASSWORD_SALT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.intercept.RunAsImplAuthenticationProvider;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import fr.openwide.core.jpa.security.access.expression.method.CoreMethodSecurityExpressionHandler;
import fr.openwide.core.jpa.security.business.authority.util.CoreAuthorityConstants;
import fr.openwide.core.jpa.security.crypto.password.CoreShaPasswordEncoder;
import fr.openwide.core.jpa.security.hierarchy.IPermissionHierarchy;
import fr.openwide.core.jpa.security.hierarchy.PermissionHierarchyImpl;
import fr.openwide.core.jpa.security.model.CorePermissionConstants;
import fr.openwide.core.jpa.security.model.NamedPermission;
import fr.openwide.core.jpa.security.runas.CoreRunAsManagerImpl;
import fr.openwide.core.jpa.security.service.AuthenticationUserNameComparison;
import fr.openwide.core.jpa.security.service.CoreAuthenticationServiceImpl;
import fr.openwide.core.jpa.security.service.CoreJpaUserDetailsServiceImpl;
import fr.openwide.core.jpa.security.service.CoreSecurityServiceImpl;
import fr.openwide.core.jpa.security.service.IAuthenticationService;
import fr.openwide.core.jpa.security.service.ICorePermissionEvaluator;
import fr.openwide.core.jpa.security.service.ISecurityService;
import fr.openwide.core.jpa.security.service.NamedPermissionFactory;
import fr.openwide.core.spring.property.service.IPropertyService;

@Configuration
@Import(DefaultJpaSecurityConfig.class)
public abstract class AbstractJpaSecurityConfig {

	private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2a?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

	@Autowired
	private DefaultJpaSecurityConfig defaultJpaSecurityConfig;
	
	@Autowired
	protected IPropertyService propertyService;

	/**
	 * N'est pas basculé en configuration car on n'est pas censé basculer d'un
	 * mode à un autre au cours de la vie de l'application. Doit être décidé au
	 * début, avec mise en place des contraintes nécessaires à la création
	 * d'utilisateur si on choisit le mode CASE INSENSITIVE. Cette méthode n'a
	 * pas besoin d'être annotée {@link Bean}
	 * 
	 * @see AuthenticationUserNameComparison
	 */
	public AuthenticationUserNameComparison authenticationUserNameComparison() {
		return AuthenticationUserNameComparison.CASE_SENSITIVE;
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder() {
			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				if (BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
					return super.matches(rawPassword, encodedPassword);
				} else {
					PasswordEncoder passwordEncoder = new CoreShaPasswordEncoder();
					return passwordEncoder.matches(rawPassword, encodedPassword);
				}
			}
		};
	}

	@Bean
	public ISecurityService securityService() {
		return new CoreSecurityServiceImpl();
	}
	
	@Bean(name = "authenticationService")
	public IAuthenticationService authenticationService() {
		return new CoreAuthenticationServiceImpl();
	}
	
	@Bean
	public UserDetailsService userDetailsService() {
		CoreJpaUserDetailsServiceImpl detailsService = new CoreJpaUserDetailsServiceImpl();
		detailsService.setAuthenticationUserNameComparison(authenticationUserNameComparison());
		return detailsService;
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			RunAsImplAuthenticationProvider runAsProvider, PasswordEncoder passwordEncoder) {
		List<AuthenticationProvider> providers = Lists.newArrayList();
		providers.add(runAsProvider);
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		providers.add(authenticationProvider);
		return new ProviderManager(providers);
	}
	
	public Class<? extends Permission> permissionClass() {
		return NamedPermission.class;
	}
	
	@Bean
	public PermissionFactory permissionFactory() {
		return new NamedPermissionFactory(permissionClass());
	}

	/**
	 * Le {@link ScopedProxyMode} est nécessaire si on désire utiliser les
	 * annotations de sécurité. En effet, l'activation des annotations de
	 * sécurité nécessite la construction du sous-système de sécurité dès le
	 * début de l'instantiation des beans (de manière à pouvoir mettre en place
	 * les intercepteurs de sécurité). Or le système de sécurité provoque le
	 * chargement du entitymanager et d'autres beans alors que leur dépendances
	 * ne sont pas prêtes. La mise en place d'un proxy permet de reporter à plus
	 * tard l'instanciation du système de sécurité.
	 */
	@Bean
	@Scope(proxyMode = ScopedProxyMode.INTERFACES)
	public abstract ICorePermissionEvaluator permissionEvaluator();

	@Bean
	public MethodSecurityExpressionHandler expressionHandler(ICorePermissionEvaluator corePermissionEvaluator) {
		CoreMethodSecurityExpressionHandler methodSecurityExpressionHandler = new CoreMethodSecurityExpressionHandler();
		methodSecurityExpressionHandler.setCorePermissionEvaluator(corePermissionEvaluator);
		return methodSecurityExpressionHandler;
	}
	
	protected String roleHierarchyAsString() {
		return defaultRoleHierarchyAsString();
	}
	
	@Bean
	public RoleHierarchy roleHierarchy() {
		RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
		roleHierarchy.setHierarchy(roleHierarchyAsString());
		return roleHierarchy;
	}

	protected String permissionHierarchyAsString() {
		return defaultPermissionHierarchyAsString();
	}

	@Bean
	@Autowired
	public IPermissionHierarchy permissionHierarchy(PermissionFactory permissionFactory) {
		PermissionHierarchyImpl hierarchy = new PermissionHierarchyImpl(permissionFactory);
		hierarchy.setHierarchy(permissionHierarchyAsString());
		return hierarchy;
	}

	@Bean
	public RunAsManager runAsManager() {
		CoreRunAsManagerImpl runAsManager = new CoreRunAsManagerImpl();
		runAsManager.setKey(defaultJpaSecurityConfig.getRunAsKey());
		return runAsManager;
	}

	@Bean
	public RunAsImplAuthenticationProvider runAsAuthenticationProvider() {
		RunAsImplAuthenticationProvider runAsAuthenticationProvider = new RunAsImplAuthenticationProvider();
		runAsAuthenticationProvider.setKey(defaultJpaSecurityConfig.getRunAsKey());
		return runAsAuthenticationProvider;
	}

	protected static String defaultPermissionHierarchyAsString() {
		return hierarchyAsStringFromMap(ImmutableMultimap.<String, String>builder()
				.put(CorePermissionConstants.ADMINISTRATION, CorePermissionConstants.WRITE)
				.put(CorePermissionConstants.WRITE, CorePermissionConstants.READ)
				.build()
		);
	}

	protected static String defaultRoleHierarchyAsString() {
		return hierarchyAsStringFromMap(ImmutableMultimap.<String, String>builder()
				.put(CoreAuthorityConstants.ROLE_SYSTEM, CoreAuthorityConstants.ROLE_ADMIN)
				.put(CoreAuthorityConstants.ROLE_ADMIN, CoreAuthorityConstants.ROLE_AUTHENTICATED)
				.put(CoreAuthorityConstants.ROLE_AUTHENTICATED, CoreAuthorityConstants.ROLE_ANONYMOUS)
				.build()
		);
	}

	protected static String hierarchyAsStringFromMap(Multimap<String, String> multimap) {
		return hierarchyAsStringFromMap(multimap.asMap());
	}

	protected static String hierarchyAsStringFromMap(Map<String, ? extends Collection<String>> map) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, ? extends Collection<String>> entry : map.entrySet()) {
			String parent = entry.getKey();
			for (String child : entry.getValue()) {
				builder.append(parent).append(" > ").append(child).append("\n");
			}
		}
		return builder.toString();
	}

}
