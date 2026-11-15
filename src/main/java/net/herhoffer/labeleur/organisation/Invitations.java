package net.herhoffer.labeleur.organisation;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Makes invitation links and sends them. */
@ApplicationScoped
public class Invitations
{
	static final Duration VALID_FOR = Duration.ofDays(14);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final Mailer mailer;
	private final String url;
	private final String secret;

	Invitations(Mailer mailer, @ConfigProperty(name = "labeleur.url") String url, @ConfigProperty(name = "labeleur.invite-secret") String secret)
	{
		this.mailer = mailer;
		this.url = url.replaceAll("/+$", "");
		this.secret = secret;
	}

	static String normalise(String email)
	{
		return email.strip().toLowerCase(Locale.ROOT);
	}

	Invitation create(String ownerId, String email, String invitedBy)
	{
		Invitation i = new Invitation();
		byte[] token = new byte[24];
		RANDOM.nextBytes(token);
		i.token = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
		i.ownerId = ownerId;
		i.email = normalise(email);
		i.invitedBy = invitedBy;
		i.createdAt = Instant.now();
		i.expiresAt = i.createdAt.plus(VALID_FOR);
		return i;
	}

	/**
	 * Opening it signed out goes to Keycloak's registration form; {@code k}
	 * lets the invited address past the registration allowlist.
	 */
	public String link(Invitation i)
	{
		return url + "/invite/" + i.token + "?k=" + InviteSignature.of(secret, i.email);
	}

	/**
	 * Whether the mail went out; the link can be passed on by hand either way.
	 */
	boolean send(Invitation i, String organisation)
	{
		String text = """
			%s has invited you to work on the labels of %s in Labeleur.

			Accept the invitation (create an account with this email address, %s, if you don't have one):
			%s

			The link is valid until %s.
			""".formatted(i.invitedBy, organisation, i.email, link(i), i.expiresAt.toString().substring(0, 10));
		try
		{
			mailer.send(Mail.withText(i.email, i.invitedBy + " invited you to " + organisation + " on Labeleur", text));
			return true;
		}
		catch (RuntimeException e)
		{
			Log.warnf(e, "Could not send an invitation");
			return false;
		}
	}
}
