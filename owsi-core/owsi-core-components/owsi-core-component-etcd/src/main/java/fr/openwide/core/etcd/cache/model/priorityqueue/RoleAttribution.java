package fr.openwide.core.etcd.cache.model.priorityqueue;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

import com.google.common.base.Objects;

public class RoleAttribution implements Serializable {

	private static final long serialVersionUID = -4848196949885956222L;

	private String owner;

	private Instant attributionInstant;

	protected RoleAttribution(String owner, Instant attributionInstant) {
		super();
		this.owner = owner;
		this.attributionInstant = attributionInstant;
	}

	protected RoleAttribution() {
		super();
	}

	public String getOwner() {
		return owner;
	}

	public Date getAttributionDate() {
		return attributionInstant != null ? Date.from(attributionInstant) : null;
	}

	public Instant getAttributionInstant() {
		return attributionInstant;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setAttributionDate(Date attributionDate) {
		this.attributionInstant = attributionDate != null ? attributionDate.toInstant() : null;
	}

	public void setAttributionInstant(Instant attributionInstant) {
		this.attributionInstant = attributionInstant;
	}

	public boolean match(String owner) {
		return Objects.equal(getOwner(), owner);
	}

	public boolean match(RoleAttribution attribution) {
		return match(attribution.getOwner());
	}

	public static final RoleAttribution from(String owner, Instant attributionInstant) {
		return new RoleAttribution(owner, attributionInstant);
	}

	public static final RoleAttribution from(String owner, Date attributionDate) {
		return new RoleAttribution(owner, attributionDate != null ? attributionDate.toInstant() : null);
	}

	@Override
	public String toString() {
		return String.format("<%s (at %tF %<tT %<tz)>", getOwner(), getAttributionDate());
	}
}
