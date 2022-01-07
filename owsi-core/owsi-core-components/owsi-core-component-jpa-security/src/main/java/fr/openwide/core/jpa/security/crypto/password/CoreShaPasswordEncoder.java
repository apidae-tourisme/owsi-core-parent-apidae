package fr.openwide.core.jpa.security.crypto.password;

import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;

public class CoreShaPasswordEncoder extends AbstractCoreMessageDigestPasswordEncoder {
	
	public CoreShaPasswordEncoder() {
		super(new MessageDigestPasswordEncoder("SHA-256"));
	}

}
