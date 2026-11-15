package net.herhoffer.labeleur.account;

import java.util.Optional;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import net.herhoffer.labeleur.organisation.Membership;
import net.herhoffer.labeleur.organisation.MembershipRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * The signed-in user, and the account whose data they work on: their own, or
 * their organisation's once they belong to one. The first time the account is
 * asked for, its sample data is set up.
 */
@RequestScoped
public class CurrentUser
{
	private final SecurityIdentity identity;
	private final Provisioner provisioner;
	private final MembershipRepository memberships;
	private String ownerId;

	CurrentUser(SecurityIdentity identity, Provisioner provisioner, MembershipRepository memberships)
	{
		this.identity = identity;
		this.provisioner = provisioner;
		this.memberships = memberships;
	}

	/** The person: their OIDC subject, stable across email and name changes. */
	public String subject()
	{
		if (identity.isAnonymous()) throw new UnauthorizedException();
		return identity.getPrincipal().getName();
	}

	/**
	 * The key of every record, setting and print job the user sees: their
	 * organisation's (its founder's subject), or their own subject without one.
	 * Sets up the account's samples on first use.
	 */
	public String ownerId()
	{
		if (ownerId == null)
		{
			String subject = subject();
			String owner = memberships.findByIdOptional(subject).map(m -> m.ownerId).orElse(subject);
			provisioner.ensure(owner);
			ownerId = owner;
		}
		return ownerId;
	}

	/** The user's membership, without one while they work on their own. */
	public Optional<Membership> membership()
	{
		return memberships.findByIdOptional(subject());
	}

	public Optional<String> name()
	{
		return claim("name").or(() -> claim("given_name"));
	}

	public Optional<String> email()
	{
		return claim("email");
	}

	private Optional<String> claim(String name)
	{
		if (identity.getPrincipal() instanceof JsonWebToken jwt) return Optional.ofNullable(jwt.<String> getClaim(name)).filter(s -> !s.isBlank());
		return Optional.empty();
	}
}
