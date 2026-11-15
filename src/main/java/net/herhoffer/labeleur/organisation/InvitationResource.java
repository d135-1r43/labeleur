package net.herhoffer.labeleur.organisation;

import java.time.Instant;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response.Status;
import net.herhoffer.labeleur.account.CurrentUser;
import net.herhoffer.labeleur.account.Profile;
import net.herhoffer.labeleur.account.ProfileRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestPath;

import static net.herhoffer.labeleur.ValidationErrors.problem;

/**
 * The invitee's side of an invitation. Only someone without an account of their
 * own can join: in practice, someone who registers from the invitation link.
 * Nothing here sets up an account, so looking at an invitation does not spoil
 * it.
 */
@Path("/invitations/{token}")
@Tag(name = "Organisation")
public class InvitationResource
{
	/**
	 * {@code problem} says why the signed-in user can't accept, absent when
	 * they can.
	 */
	public record InvitationDetails(String organisation, String invitedBy, String email, String signedInAs, Instant expiresAt, String problem)
	{
	}

	private final CurrentUser user;
	private final InvitationRepository invitations;
	private final MembershipRepository memberships;
	private final ProfileRepository profiles;

	InvitationResource(CurrentUser user, InvitationRepository invitations, MembershipRepository memberships, ProfileRepository profiles)
	{
		this.user = user;
		this.invitations = invitations;
		this.memberships = memberships;
		this.profiles = profiles;
	}

	@GET
	@Operation(summary = "What the invitation is for, and whether the signed-in user can accept it")
	public InvitationDetails get(@RestPath String token)
	{
		Invitation i = open(token);
		Profile org = profiles.findById(i.ownerId);
		return new InvitationDetails(org == null ? i.invitedBy : org.name, i.invitedBy, i.email, user.email().orElse(null), i.expiresAt, whyNot(i));
	}

	@POST
	@Path("/accept")
	@Operation(summary = "Join the organisation; from then on the user works on its data")
	public void accept(@RestPath String token)
	{
		String me = user.subject();
		QuarkusTransaction.requiringNew().run(() -> {
			Invitation i = open(token);
			String reason = whyNot(i);
			if (reason != null) throw problem(Status.CONFLICT, reason);
			Membership m = new Membership();
			m.userId = me;
			m.ownerId = i.ownerId;
			m.email = i.email;
			m.name = user.name().orElse(null);
			m.joinedAt = Instant.now();
			memberships.persist(m);
			invitations.delete(i);
		});
	}

	private Invitation open(String token)
	{
		Invitation i = invitations.findById(token);
		if (i == null) throw new NotFoundException("This invitation was withdrawn or has already been accepted");
		if (i.expired()) throw problem(Status.GONE, "This invitation has expired. Ask for a new one.");
		return i;
	}

	private String whyNot(Invitation i)
	{
		String me = user.subject();
		if (!i.email.equalsIgnoreCase(user.email().orElse(""))) return "This invitation is for " + i.email + ". Sign in with that address to accept it.";
		Membership m = memberships.findById(me);
		if (m != null) return m.ownerId.equals(i.ownerId) ? "You are already a member." : "You already belong to another organisation.";
		if (profiles.findById(me) != null) return "You already have a Labeleur account of your own. Only new accounts can join an organisation for now.";
		return null;
	}
}
