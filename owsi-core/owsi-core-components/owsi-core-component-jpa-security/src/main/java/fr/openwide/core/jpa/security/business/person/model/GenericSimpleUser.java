package fr.openwide.core.jpa.security.business.person.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.bindgen.Bindable;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.SortableField;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.openwide.core.jpa.search.util.HibernateSearchAnalyzer;
import fr.openwide.core.spring.notification.model.INotificationRecipient;

@MappedSuperclass
@Bindable
public abstract class GenericSimpleUser<U extends GenericSimpleUser<U, G>, G extends GenericUserGroup<G, U>>
		extends GenericUser<U, G>
		implements ISimpleUser, INotificationRecipient {
	
	private static final long serialVersionUID = 4869548461178261021L;
	
	public static final String FIRST_NAME_SORT_FIELD_NAME = "firstNameSort";
	public static final String LAST_NAME_SORT_FIELD_NAME = "lastNameSort";
	
	@Column(nullable = false)
	@Fields({
			@Field(analyzer = @Analyzer(definition = HibernateSearchAnalyzer.TEXT)),
			@Field(name = FIRST_NAME_SORT_FIELD_NAME, analyzer = @Analyzer(definition = HibernateSearchAnalyzer.TEXT_SORT))
	})
	@SortableField(forField = FIRST_NAME_SORT_FIELD_NAME)
	private String firstName;
	
	@Column(nullable = false)
	@Fields({
			@Field(analyzer = @Analyzer(definition = HibernateSearchAnalyzer.TEXT)),
			@Field(name = LAST_NAME_SORT_FIELD_NAME, analyzer = @Analyzer(definition = HibernateSearchAnalyzer.TEXT_SORT))
	})
	@SortableField(forField = LAST_NAME_SORT_FIELD_NAME)
	private String lastName;
	
	@Field(analyzer = @Analyzer(definition = HibernateSearchAnalyzer.TEXT))
	private String email;
	
	private String phoneNumber;
	
	private String gsmNumber;
	
	private String faxNumber;
	
	public GenericSimpleUser() {
		super();
	}
	
	public GenericSimpleUser(String userName, String firstName, String lastName, String passwordHash) {
		super(userName, passwordHash);
		setFirstName(firstName);
		setLastName(lastName);
	}
	
	/*
	 * Works around a bindgen bug, where bindgen seems unable to substitute a concrete type to the "G" type parameter if we don't override this method here.
	 */
	@Override
	public Set<G> getGroups() {
		return super.getGroups(); //NOSONAR
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Override
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Transient
	@Override
	public String getFullName() {
		StringBuilder builder = new StringBuilder();
		if(firstName != null) {
			builder.append(firstName);
			builder.append(" ");
		}
		if(lastName != null && !lastName.equalsIgnoreCase(firstName)) {
			builder.append(lastName);
		}
		return builder.toString().trim();
	}

	@Override
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setGsmNumber(String gsmNumber) {
		this.gsmNumber = gsmNumber;
	}

	public String getGsmNumber() {
		return gsmNumber;
	}

	public String getFaxNumber() {
		return faxNumber;
	}

	public void setFaxNumber(String faxNumber) {
		this.faxNumber = faxNumber;
	}

	@Override
	public int compareTo(U user) {
		if(this.equals(user)) {
			return 0;
		}
		
		if(DEFAULT_STRING_COLLATOR.compare(this.getLastName(), user.getLastName()) == 0) {
			return DEFAULT_STRING_COLLATOR.compare(this.getFirstName(), user.getFirstName());
		}
		return DEFAULT_STRING_COLLATOR.compare(this.getLastName(), user.getLastName());
	}

	@Override
	@Transient
	@JsonIgnore
	public boolean isNotificationEnabled() {
		// impl??mentation par d??faut ; d??pend de l'??tat de l'utilisateur
		return isActive();
	}

}
