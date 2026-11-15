package net.herhoffer.labeleur.organisation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response.Status;
import net.herhoffer.labeleur.account.CurrentUser;
import net.herhoffer.labeleur.account.ProfileRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestPath;

import static net.herhoffer.labeleur.ValidationErrors.problem;

/**
 * Who shares the account. Alone, a user works on their own data; inviting
 * someone makes the account an organisation, and everyone in it may do
 * everything: print, edit records and settings, invite and remove others.
 */
@Path("/organisation")
@Tag(name = "Organisation")
public class MembersResource
{
	/** {@code founder}: whose account became the organisation; stays in it. */
	public record MemberView(String id, String name, String email, boolean founder, boolean you)
	{
	}

	/** Pending until accepted; {@code link} can be passed on by hand. */
	public record InvitationView(String token, String email, String invitedBy, Instant createdAt, Instant expiresAt, String link, Boolean emailSent)
	{
	}

	public record MembersView(List<MemberView> members, List<InvitationView> invitations)
	{
	}

	public record InviteRequest(@NotBlank @Email @Size(max = 320) String email)
	{
	}

	private final CurrentUser user;
	private final MembershipRepository memberships;
	private final InvitationRepository invitationRepository;
	private final ProfileRepository profiles;
	private final Invitations invitations;

	MembersResource(CurrentUser user, MembershipRepository memberships, InvitationRepository invitationRepository, ProfileRepository profiles, Invitations invitations)
	{
		this.user = user;
		this.memberships = memberships;
		this.invitationRepository = invitationRepository;
		this.profiles = profiles;
		this.invitations = invitations;
	}

	@GET
	@Path("/members")
	@Operation(summary = "The people sharing this account, and the invitations not yet accepted")
	public MembersView members()
	{
		String owner = user.ownerId();
		String me = user.subject();
		List<MemberView> members = new ArrayList<>();
		for (Membership m : memberships.of(owner))
			members.add(new MemberView(m.userId, m.name, m.email, m.founder(), m.userId.equals(me)));
		// Alone on their own account: just them.
		if (members.isEmpty()) members.add(new MemberView(me, user.name().orElse(null), user.email().orElse(null), true, true));
		List<InvitationView> open = invitationRepository.of(owner).stream().filter(i -> !i.expired()).map(i -> view(i, null)).toList();
		return new MembersView(members, open);
	}

	@POST
	@Path("/invitations")
	@Operation(summary = "Invite someone by email; the first invitation turns the account into an organisation")
	public InvitationView invite(@Valid @NotNull InviteRequest request)
	{
		String owner = user.ownerId();
		String email = Invitations.normalise(request.email());
		String me = user.subject();
		String myName = user.name().or(user::email).orElse("A Labeleur user");
		Invitation created = QuarkusTransaction.requiringNew().call(() -> {
			if (memberships.of(owner).stream().anyMatch(m -> email.equalsIgnoreCase(m.email))) throw problem(Status.CONFLICT, email + " is already a member");
			if (email.equalsIgnoreCase(user.email().orElse(""))) throw problem(Status.CONFLICT, "That's you");
			// Inviting makes the account an organisation, with this user in it.
			if (memberships.findById(me) == null)
			{
				Membership founder = new Membership();
				founder.userId = me;
				founder.ownerId = owner;
				founder.email = user.email().orElse(null);
				founder.name = user.name().orElse(null);
				founder.joinedAt = Instant.now();
				memberships.persist(founder);
			}
			// Inviting the same address again sends a fresh link.
			invitationRepository.find(owner, email).ifPresent(invitationRepository::delete);
			invitationRepository.flush();
			Invitation i = invitations.create(owner, email, myName);
			invitationRepository.persist(i);
			return i;
		});
		boolean sent = invitations.send(created, profiles.findById(owner).name);
		return view(created, sent);
	}

	@DELETE
	@Path("/invitations/{token}")
	@Operation(summary = "Withdraw an invitation")
	public void revoke(@RestPath String token)
	{
		String owner = user.ownerId();
		QuarkusTransaction.requiringNew().run(() -> {
			Invitation i = invitationRepository.findById(token);
			if (i == null || !i.ownerId.equals(owner)) throw new NotFoundException("No such invitation");
			invitationRepository.delete(i);
		});
	}

	@DELETE
	@Path("/members/{id}")
	@Operation(summary = "Remove someone from the organisation (or leave it); they go back to an account of their own")
	public void remove(@RestPath String id)
	{
		String owner = user.ownerId();
		QuarkusTransaction.requiringNew().run(() -> {
			Membership m = memberships.findById(id);
			if (m == null || !m.ownerId.equals(owner)) throw new NotFoundException("No such member");
			if (m.founder()) throw problem(Status.CONFLICT, "The organisation's data is the founder's account: they stay in it");
			memberships.delete(m);
		});
	}

	private InvitationView view(Invitation i, Boolean sent)
	{
		return new InvitationView(i.token, i.email, i.invitedBy, i.createdAt, i.expiresAt, invitations.link(i), sent);
	}
}
