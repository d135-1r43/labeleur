package net.herhoffer.labeleur.organisation;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A user who works on an organisation's data rather than their own. The
 * founder's row has {@code ownerId == userId}; the organisation's data is keyed
 * by that id.
 */
@Entity
public class Membership extends PanacheEntityBase
{
	/** The member's OIDC subject. */
	@Id
	public String userId;

	/** Whose data the member works on: the founder's subject. */
	public String ownerId;

	/** As of joining, to list the members. */
	public String email;
	public String name;
	public Instant joinedAt;

	public boolean founder()
	{
		return userId.equals(ownerId);
	}
}
