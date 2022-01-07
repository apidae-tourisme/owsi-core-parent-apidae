package fr.openwide.core.jpa.security.crypto.password;

import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AbstractCoreMessageDigestPasswordEncoder implements PasswordEncoder {

	private MessageDigestPasswordEncoder delegate;

	private String salt;

	protected AbstractCoreMessageDigestPasswordEncoder(MessageDigestPasswordEncoder delegate) {
		this.delegate = delegate;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		return delegate.encode(rawPassword.toString());
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		/**https://stackoverflow.com/questions/55031118/why-does-springs-messagedigestpasswordencoder-take-into-the-salt-the-exam
		passage a spring security ==> le salt est passé en tant qu'expression type {salt} directement dans le password
		https://stackoverflow.com/questions/63212255/how-to-add-salt-into-md5-encryption-in-spring-security-5-x
		 on peut plus configurer de salt dans spring security 5
		====> en résumé on peux décoder avec l'ancien salt pour les anciens compte seulement, pour les nouveaux ça sera avec du salt généré
		**/
		return delegate.matches(rawPassword.toString(), encodedPassword);
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

}
