package net.herhoffer.labeleur.organisation;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An open invitation to join an organisation; gone once accepted or revoked.
 */
@Entity
public class Invitation extends PanacheEntityBase
{
	/** The secret in the invitation link. */
	@Id
	public String token;

	/** The organisation: its founder's subject. */
	public String ownerId;

	/** Lower case; only a user signed in with it can accept. */
	public String email;

	public String invitedBy;
	public Instant createdAt;
	public Instant expiresAt;

	public boolean expired()
	{
		return Instant.now().isAfter(expiresAt);
	}
}
