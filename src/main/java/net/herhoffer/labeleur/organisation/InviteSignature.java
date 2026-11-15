package net.herhoffer.labeleur.organisation;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Proves to Keycloak that an email address was invited, so its registration
 * allowlist lets exactly that address register. Keycloak computes the same
 * value with the secret both share (the {@code labeleur} client secret); see
 * {@code net.herhoffer.labeleur.keycloak.InviteSignature}, which must stay in
 * step with this.
 */
public final class InviteSignature
{
	/** The authorization request parameter Keycloak reads it from. */
	public static final String PARAM = "labeleur_invite";

	private InviteSignature()
	{
	}

	public static String of(String secret, String email)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] sig = mac.doFinal(("labeleur-invite:" + email.strip().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
