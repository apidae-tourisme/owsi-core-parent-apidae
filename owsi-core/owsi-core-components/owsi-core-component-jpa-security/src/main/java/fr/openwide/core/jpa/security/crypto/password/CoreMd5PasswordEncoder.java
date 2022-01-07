package fr.openwide.core.jpa.security.crypto.password;

import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;

public class CoreMd5PasswordEncoder extends AbstractCoreMessageDigestPasswordEncoder {

	public CoreMd5PasswordEncoder() {
		super(new MessageDigestPasswordEncoder("MD5"));
	}

}
